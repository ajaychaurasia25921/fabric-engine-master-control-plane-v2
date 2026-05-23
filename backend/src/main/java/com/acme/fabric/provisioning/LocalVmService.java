package com.acme.fabric.provisioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    public LocalVmService(
            @Value("${reactor.vm.execution-enabled:false}") boolean executionEnabled,
            @Value("${reactor.vm.root:${user.home}/ReactorVMs}") String vmRoot
    ) {
        this.executionEnabled = executionEnabled;
        this.vmRoot = Path.of(vmRoot);
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
            case "VMWARE" -> detectBinary("vmrun", "VMware Workstation/Fusion vmrun CLI");
            case "QEMU" -> detectBinary("qemu-system-x86_64", "QEMU system emulator");
            default -> new LocalVmProviderStatus(provider, false, "", "Unknown provider");
        };
    }

    private LocalVmProviderStatus detectBinary(String binary, String notes) {
        ProcessResult result = run(List.of("sh", "-lc", "command -v " + binary));
        String path = result.stdout().trim();
        return new LocalVmProviderStatus(binary.equals("vmrun") ? "VMWARE" : "QEMU", !path.isBlank(), path, notes);
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
                "vmrun create \"" + vmx + "\"",
                "vmrun start \"" + vmx + "\" nogui"
        );
        if (!executionEnabled) {
            return dryRun(request, "VMWARE", commands, List.of(
                    "Set REACTOR_VM_EXECUTION_ENABLED=true and run backend on the physical host.",
                    "Install VMware Fusion/Workstation and ensure vmrun is on PATH.",
                    "Review generated VMX at " + vmx
            ));
        }
        ProcessResult start = request.startAfterCreate() == Boolean.TRUE
                ? run(List.of("vmrun", "start", vmx.toString(), "nogui"))
                : new ProcessResult("", "");
        return result(request, "VMWARE", true, commands, start);
    }

    private LocalVmCommandResult createQemu(LocalVmCreateRequest request) throws IOException {
        Path vmDir = vmRoot.resolve(safeName(request.vmName()));
        Files.createDirectories(vmDir);
        Path disk = vmDir.resolve(safeName(request.vmName()) + ".qcow2");
        List<String> commands = List.of(
                "qemu-img create -f qcow2 \"" + disk + "\" " + valueOrDefault(request.diskGb(), 40) + "G",
                "qemu-system-x86_64 -m " + valueOrDefault(request.memoryMb(), 4096)
                        + " -smp " + valueOrDefault(request.cpuCores(), 2)
                        + " -drive file=\"" + disk + "\",if=virtio"
                        + qemuIsoArg(request.isoPath())
                        + " -netdev user,id=n0 -device virtio-net-pci,netdev=n0"
        );
        if (!executionEnabled) {
            return dryRun(request, "QEMU", commands, List.of(
                    "Set REACTOR_VM_EXECUTION_ENABLED=true and run backend on the physical host.",
                    "Install qemu-img and qemu-system-x86_64.",
                    "Use the generated command plan to create/start the VM."
            ));
        }
        ProcessResult diskResult = run(List.of("qemu-img", "create", "-f", "qcow2", disk.toString(), valueOrDefault(request.diskGb(), 40) + "G"));
        return result(request, "QEMU", true, commands, diskResult);
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

    private ProcessResult run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return new ProcessResult(stdout, stderr);
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            return new ProcessResult("", error.getMessage());
        }
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

    private record ProcessResult(String stdout, String stderr) {
    }
}
