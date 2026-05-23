package com.acme.fabric.provisioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.acme.fabric.domain.FabricModels.LocalVmCommandResult;
import com.acme.fabric.domain.FabricModels.LocalVmCreateRequest;
import com.acme.fabric.domain.FabricModels.LocalVmProviderStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class LocalVmService {
    private final boolean executionEnabled;
    private final Path vmRoot;
    private final String vmrunBinary;
    private final String vmwareDiskManagerBinary;
    private final String qemuImgBinary;
    private final String qemuSystemBinary;

    public LocalVmService(
            @Value("${reactor.vm.execution-enabled:false}") boolean executionEnabled,
            @Value("${reactor.vm.root:${user.home}/ReactorVMs}") String vmRoot,
            @Value("${reactor.vm.vmrun-binary:vmrun}") String vmrunBinary,
            @Value("${reactor.vm.vmware-diskmanager-binary:vmware-vdiskmanager}") String vmwareDiskManagerBinary,
            @Value("${reactor.vm.qemu-img-binary:qemu-img}") String qemuImgBinary,
            @Value("${reactor.vm.qemu-system-binary:qemu-system-x86_64}") String qemuSystemBinary
    ) {
        this.executionEnabled = executionEnabled;
        this.vmRoot = Path.of(vmRoot);
        this.vmrunBinary = vmrunBinary;
        this.vmwareDiskManagerBinary = vmwareDiskManagerBinary;
        this.qemuImgBinary = qemuImgBinary;
        this.qemuSystemBinary = qemuSystemBinary;
    }

    public Flux<LocalVmProviderStatus> providers() {
        return Flux.just("VMWARE", "QEMU")
                .flatMap(provider -> Mono.fromCallable(() -> detect(provider)).subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<LocalVmCommandResult> create(LocalVmCreateRequest request) {
        return Mono.fromCallable(() -> createBlocking(request)).subscribeOn(Schedulers.boundedElastic());
    }

    private LocalVmProviderStatus detect(String provider) {
        return switch (provider) {
            case "VMWARE" -> detectVmware();
            case "QEMU" -> detectQemu();
            default -> new LocalVmProviderStatus(provider, false, "", "Unknown provider");
        };
    }

    private LocalVmProviderStatus detectVmware() {
        String vmrun = resolveBinary(vmrunBinary, List.of(
                "/Applications/VMware Fusion.app/Contents/Library/vmrun",
                "/usr/bin/vmrun",
                "/usr/local/bin/vmrun",
                "/opt/homebrew/bin/vmrun"
        ));
        String diskManager = resolveBinary(vmwareDiskManagerBinary, List.of(
                "/Applications/VMware Fusion.app/Contents/Library/vmware-vdiskmanager",
                "/usr/bin/vmware-vdiskmanager",
                "/usr/local/bin/vmware-vdiskmanager",
                "/opt/homebrew/bin/vmware-vdiskmanager"
        ));
        boolean available = !vmrun.isBlank() && !diskManager.isBlank();
        return new LocalVmProviderStatus(
                "VMWARE",
                available,
                available ? vmrun + " | " + diskManager : firstNonBlank(vmrun, diskManager),
                available
                        ? "VMware Fusion/Workstation detected. Real VMX/VMDK creation can run when REACTOR_VM_EXECUTION_ENABLED=true."
                        : "Install VMware Fusion/Workstation and expose vmrun plus vmware-vdiskmanager on PATH."
        );
    }

    private LocalVmProviderStatus detectQemu() {
        String qemuImg = resolveBinary(qemuImgBinary, List.of("/usr/bin/qemu-img", "/usr/local/bin/qemu-img", "/opt/homebrew/bin/qemu-img"));
        String qemuSystem = resolveBinary(qemuSystemBinary, List.of("/usr/bin/qemu-system-x86_64", "/usr/local/bin/qemu-system-x86_64", "/opt/homebrew/bin/qemu-system-x86_64"));
        boolean available = !qemuImg.isBlank() && !qemuSystem.isBlank();
        return new LocalVmProviderStatus(
                "QEMU",
                available,
                available ? qemuImg + " | " + qemuSystem : firstNonBlank(qemuImg, qemuSystem),
                available
                        ? "QEMU detected. Real qcow2 creation and optional launch can run when REACTOR_VM_EXECUTION_ENABLED=true."
                        : "Install qemu-img and qemu-system-x86_64, then retry."
        );
    }

    private String resolveBinary(String configuredBinary, List<String> fallbacks) {
        if (configuredBinary != null && configuredBinary.contains("/") && Files.isExecutable(Path.of(configuredBinary))) {
            return configuredBinary;
        }
        ProcessResult result = runAndWait(List.of("sh", "-lc", "command -v " + shellQuote(configuredBinary)));
        String path = result.stdout().trim();
        if (!path.isBlank()) {
            return path;
        }
        return fallbacks.stream()
                .filter(candidate -> Files.isExecutable(Path.of(candidate)))
                .findFirst()
                .orElse("");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private LocalVmCommandResult createBlocking(LocalVmCreateRequest request) throws IOException {
        Files.createDirectories(vmRoot);
        String provider = request.provider().toUpperCase(Locale.ROOT);
        return switch (provider) {
            case "VMWARE" -> createVmware(request);
            case "QEMU" -> createQemu(request);
            default -> new LocalVmCommandResult(
                    "vm-" + UUID.randomUUID().toString().substring(0, 8),
                    "REJECTED",
                    false,
                    provider,
                    List.of(),
                    "",
                    "Unsupported provider. Use VMWARE or QEMU.",
                    List.of("Install VMware Fusion/Workstation vmrun or QEMU, then retry.")
            );
        };
    }

    private LocalVmCommandResult createVmware(LocalVmCreateRequest request) throws IOException {
        Path vmDir = vmRoot.resolve(safeName(request.vmName()));
        Files.createDirectories(vmDir);
        Path vmx = vmDir.resolve(safeName(request.vmName()) + ".vmx");
        Path vmdk = vmDir.resolve(safeName(request.vmName()) + ".vmdk");
        String vmxContent = """
                .encoding = "UTF-8"
                config.version = "8"
                virtualHW.version = "20"
                displayName = "%s"
                guestOS = "%s"
                memsize = "%d"
                numvcpus = "%d"
                ethernet0.present = "TRUE"
                ethernet0.connectionType = "%s"
                scsi0.present = "TRUE"
                scsi0.virtualDev = "lsilogic"
                scsi0:0.present = "TRUE"
                scsi0:0.fileName = "%s.vmdk"
                ide1:0.present = "%s"
                ide1:0.fileName = "%s"
                ide1:0.deviceType = "cdrom-image"
                tools.syncTime = "TRUE"
                """.formatted(
                request.vmName(),
                request.guestOsType(),
                valueOrDefault(request.memoryMb(), 4096),
                valueOrDefault(request.cpuCores(), 2),
                vmwareNetwork(request.networkMode()),
                safeName(request.vmName()),
                request.isoPath() == null || request.isoPath().isBlank() ? "FALSE" : "TRUE",
                request.isoPath() == null ? "" : request.isoPath()
        );
        Files.writeString(vmx, vmxContent, StandardCharsets.UTF_8);
        List<String> commands = List.of(
                shellJoin(List.of(vmwareDiskManagerBinary, "-c", "-s", valueOrDefault(request.diskGb(), 40) + "GB", "-a", "lsilogic", "-t", "0", vmdk.toString())),
                shellJoin(List.of(vmrunBinary, "start", vmx.toString(), "nogui"))
        );
        if (!executionEnabled) {
            return dryRun(request, "VMWARE", commands, List.of(
                    "Set REACTOR_VM_EXECUTION_ENABLED=true and run backend on the physical host.",
                    "Install VMware Fusion/Workstation and ensure vmrun is on PATH.",
                    "Review generated VMX at " + vmx
            ));
        }
        String diskManager = resolveBinary(vmwareDiskManagerBinary, List.of(
                "/Applications/VMware Fusion.app/Contents/Library/vmware-vdiskmanager",
                "/usr/local/bin/vmware-vdiskmanager",
                "/opt/homebrew/bin/vmware-vdiskmanager"
        ));
        String vmrun = resolveBinary(vmrunBinary, List.of(
                "/Applications/VMware Fusion.app/Contents/Library/vmrun",
                "/usr/local/bin/vmrun",
                "/opt/homebrew/bin/vmrun"
        ));
        if (diskManager.isBlank() || vmrun.isBlank()) {
            return missingToolResult(request, "VMWARE", commands, "VMware vmrun and vmware-vdiskmanager are required for real VM creation.");
        }
        List<ProcessResult> results = new ArrayList<>();
        if (!Files.exists(vmdk)) {
            results.add(runAndWait(List.of(diskManager, "-c", "-s", valueOrDefault(request.diskGb(), 40) + "GB", "-a", "lsilogic", "-t", "0", vmdk.toString())));
        }
        if (request.startAfterCreate() == Boolean.TRUE) {
            results.add(runAndWait(List.of(vmrun, "start", vmx.toString(), "nogui")));
        }
        return result(request, "VMWARE", true, commands, combine(results));
    }

    private LocalVmCommandResult createQemu(LocalVmCreateRequest request) throws IOException {
        Path vmDir = vmRoot.resolve(safeName(request.vmName()));
        Files.createDirectories(vmDir);
        Path disk = vmDir.resolve(safeName(request.vmName()) + ".qcow2");
        List<String> commands = List.of(
                shellJoin(List.of(qemuImgBinary, "create", "-f", "qcow2", disk.toString(), valueOrDefault(request.diskGb(), 40) + "G")),
                shellJoin(qemuStartCommand(request, disk))
        );
        if (!executionEnabled) {
            return dryRun(request, "QEMU", commands, List.of(
                    "Set REACTOR_VM_EXECUTION_ENABLED=true and run backend on the physical host.",
                    "Install qemu-img and qemu-system-x86_64.",
                    "Use the generated command plan to create/start the VM."
            ));
        }
        String qemuImg = resolveBinary(qemuImgBinary, List.of("/usr/local/bin/qemu-img", "/opt/homebrew/bin/qemu-img"));
        String qemuSystem = resolveBinary(qemuSystemBinary, List.of("/usr/local/bin/qemu-system-x86_64", "/opt/homebrew/bin/qemu-system-x86_64"));
        if (qemuImg.isBlank() || qemuSystem.isBlank()) {
            return missingToolResult(request, "QEMU", commands, "qemu-img and qemu-system-x86_64 are required for real VM creation.");
        }
        List<ProcessResult> results = new ArrayList<>();
        if (!Files.exists(disk)) {
            results.add(runAndWait(List.of(qemuImg, "create", "-f", "qcow2", disk.toString(), valueOrDefault(request.diskGb(), 40) + "G")));
        }
        if (request.startAfterCreate() == Boolean.TRUE) {
            List<String> startCommand = qemuStartCommand(request, disk);
            startCommand.set(0, qemuSystem);
            results.add(runDetached(startCommand, vmDir.resolve("qemu.log")));
        }
        return result(request, "QEMU", true, commands, combine(results));
    }

    private LocalVmCommandResult dryRun(LocalVmCreateRequest request, String provider, List<String> commands, List<String> nextSteps) {
        return new LocalVmCommandResult(
                "vm-" + UUID.randomUUID().toString().substring(0, 8),
                "DRY_RUN_READY",
                false,
                provider,
                commands,
                "Execution disabled at " + Instant.now(),
                "",
                nextSteps
        );
    }

    private LocalVmCommandResult result(LocalVmCreateRequest request, String provider, boolean executed, List<String> commands, ProcessResult processResult) {
        return new LocalVmCommandResult(
                "vm-" + Math.abs(request.vmName().hashCode()),
                executed ? "CREATED" : "DRY_RUN_READY",
                executed,
                provider,
                commands,
                processResult.stdout(),
                processResult.stderr(),
                List.of("Register VM MAC and dual IP in Reactor fabric.", "Open VDI or CLI once the guest OS is installed.")
        );
    }

    private LocalVmCommandResult missingToolResult(LocalVmCreateRequest request, String provider, List<String> commands, String message) {
        return new LocalVmCommandResult(
                "vm-" + Math.abs(request.vmName().hashCode()),
                "REAL_EXECUTION_BLOCKED",
                false,
                provider,
                commands,
                "",
                message,
                List.of(
                        "Install the provider tooling on the physical host.",
                        "Run Reactor backend directly on the host, not inside Docker, or mount provider binaries into the backend container.",
                        "Retry with REACTOR_VM_EXECUTION_ENABLED=true after provider detection shows Available."
                )
        );
    }

    private ProcessResult runAndWait(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult("", "Command timed out after 120 seconds: " + shellJoin(command));
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(stdout, stderr);
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            return new ProcessResult("", error.getMessage());
        }
    }

    private ProcessResult runDetached(List<String> command, Path logFile) {
        try {
            Files.createDirectories(logFile.getParent());
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
                    .redirectError(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
                    .start();
            return new ProcessResult("Started detached process pid=" + process.pid() + ", log=" + logFile, "");
        } catch (IOException error) {
            return new ProcessResult("", error.getMessage());
        }
    }

    private ProcessResult combine(List<ProcessResult> results) {
        String stdout = results.stream().map(ProcessResult::stdout).filter(value -> !value.isBlank()).reduce("", (left, right) -> left + right + "\n");
        String stderr = results.stream().map(ProcessResult::stderr).filter(value -> !value.isBlank()).reduce("", (left, right) -> left + right + "\n");
        return new ProcessResult(stdout, stderr);
    }

    private String safeName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String vmwareNetwork(String networkMode) {
        if ("BRIDGED".equalsIgnoreCase(networkMode)) return "bridged";
        if ("HOST_ONLY".equalsIgnoreCase(networkMode)) return "hostonly";
        return "nat";
    }

    private String qemuIsoArg(String isoPath) {
        return isoPath == null || isoPath.isBlank() ? "" : " -cdrom \"" + isoPath + "\" -boot d";
    }

    private List<String> qemuStartCommand(LocalVmCreateRequest request, Path disk) {
        List<String> command = new ArrayList<>(List.of(
                qemuSystemBinary,
                "-m", String.valueOf(valueOrDefault(request.memoryMb(), 4096)),
                "-smp", String.valueOf(valueOrDefault(request.cpuCores(), 2)),
                "-drive", "file=" + disk + ",if=virtio",
                "-netdev", "user,id=n0",
                "-device", "virtio-net-pci,netdev=n0"
        ));
        if (request.isoPath() != null && !request.isoPath().isBlank()) {
            command.addAll(List.of("-cdrom", request.isoPath(), "-boot", "d"));
        }
        return command;
    }

    private String shellJoin(List<String> command) {
        return command.stream().map(this::shellQuote).reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private String shellQuote(String value) {
        if (value == null || value.isBlank()) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private record ProcessResult(String stdout, String stderr) {
    }
}
