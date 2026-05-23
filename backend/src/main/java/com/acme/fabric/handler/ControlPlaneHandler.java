package com.acme.fabric.handler;

import static com.acme.fabric.domain.FabricModels.FabricEventType.NODE_STATUS;
import static com.acme.fabric.domain.FabricModels.FabricEventType.PIPELINE_UPDATE;
import static com.acme.fabric.domain.FabricModels.FabricEventType.SECURITY_ALERT;
import static com.acme.fabric.domain.FabricModels.NodeStatus.DEGRADED;
import static com.acme.fabric.domain.FabricModels.NodeStatus.INITIALIZING;
import static com.acme.fabric.domain.FabricModels.NodeStatus.REMEDIATING;
import static com.acme.fabric.domain.FabricModels.Severity.CRITICAL;
import static com.acme.fabric.domain.FabricModels.Severity.INFO;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.acme.fabric.domain.FabricModels.AllocationMetadata;
import com.acme.fabric.domain.FabricModels.AiGuidePrompt;
import com.acme.fabric.domain.FabricModels.AiGuideResponse;
import com.acme.fabric.domain.FabricModels.AgentActionProposal;
import com.acme.fabric.domain.FabricModels.ComputeJobStatus;
import com.acme.fabric.domain.FabricModels.DeviceRegistrationRequest;
import com.acme.fabric.domain.FabricModels.DeviceRegistrationResponse;
import com.acme.fabric.domain.FabricModels.FabricEvent;
import com.acme.fabric.domain.FabricModels.FirewallRuleEntity;
import com.acme.fabric.domain.FabricModels.HardwareOverview;
import com.acme.fabric.domain.FabricModels.HoneypotIncidentRecord;
import com.acme.fabric.domain.FabricModels.IdentityScrambleIntent;
import com.acme.fabric.domain.FabricModels.LocalVmCreateRequest;
import com.acme.fabric.domain.FabricModels.PacketTraceRequest;
import com.acme.fabric.domain.FabricModels.PacketTraceResult;
import com.acme.fabric.domain.FabricModels.PipelineRequest;
import com.acme.fabric.domain.FabricModels.PipelineResponse;
import com.acme.fabric.domain.FabricModels.QuantumCircuitExecutionRequest;
import com.acme.fabric.domain.FabricModels.ResolvedHop;
import com.acme.fabric.domain.FabricModels.RuntimeState;
import com.acme.fabric.domain.FabricModels.ScrambleExecutionSummary;
import com.acme.fabric.domain.FabricModels.ServerInstanceRecord;
import com.acme.fabric.domain.FabricModels.ServerProvisionIntent;
import com.acme.fabric.domain.FabricModels.ServerProvisionStatus;
import com.acme.fabric.domain.FabricModels.ServerSpawnRequest;
import com.acme.fabric.domain.FabricModels.StagedAssetResponse;
import com.acme.fabric.domain.FabricModels.StagedFileMetadata;
import com.acme.fabric.domain.FabricModels.TelnetCommandPayload;
import com.acme.fabric.domain.FabricModels.TelnetCommandResponse;
import com.acme.fabric.provisioning.LocalVmService;
import com.acme.fabric.stream.FabricEventBus;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ControlPlaneHandler {
    private final FabricEventBus eventBus;
    private final LocalVmService localVmService;
    private final CopyOnWriteArrayList<FirewallRuleEntity> firewallRules = new CopyOnWriteArrayList<>(List.of(
            new FirewallRuleEntity("10.0.0.0/8", "23/TCP", "DROP"),
            new FirewallRuleEntity("192.168.1.0/24", "22/TCP", "ACCEPT")
    ));
    private final CopyOnWriteArrayList<HoneypotIncidentRecord> honeypotIncidents = new CopyOnWriteArrayList<>(List.of(
            new HoneypotIncidentRecord(UUID.randomUUID().toString(), "185.220.101.4", "Legacy Telnet Root Prompt", 23, Instant.now())
    ));

    public ControlPlaneHandler(FabricEventBus eventBus, LocalVmService localVmService) {
        this.eventBus = eventBus;
        this.localVmService = localVmService;
    }

    public Mono<ServerResponse> provisionServer(ServerRequest request) {
        return request.bodyToMono(ServerProvisionIntent.class)
                .map(intent -> {
                    String serverId = UUID.randomUUID().toString();
                    String ip = intent.fabricIpAddress() == null || intent.fabricIpAddress().isBlank()
                            ? "10.194." + (Math.abs(serverId.hashCode()) % 220 + 10) + "." + (Math.abs(intent.serverName().hashCode()) % 220 + 10)
                            : intent.fabricIpAddress();
                    String placement = intent.placementMode() == null ? "PHYSICAL_HOST_VM" : intent.placementMode();
                    List<String> nextActions = provisioningActions(intent, placement);
                    eventBus.publish(new FabricEvent(
                            UUID.randomUUID().toString(),
                            NODE_STATUS,
                            INFO,
                            intent.serverName(),
                            INITIALIZING,
                            "Server provisioning pipeline accepted for " + intent.serverName(),
                            "Bootstrap allocation assigned to " + ip,
                            Instant.now()
                    ));
                    return new ServerProvisionStatus(
                            serverId,
                            new AllocationMetadata(ip, intent.internetIpAddress(), intent.registeredMacAddress(), placement, Instant.now()),
                            RuntimeState.INITIALIZING,
                            nextActions
                    );
                })
                .flatMap(status -> ServerResponse.status(CREATED).contentType(APPLICATION_JSON).bodyValue(status));
    }

    public Mono<ServerResponse> registerDevice(ServerRequest request) {
        return request.bodyToMono(DeviceRegistrationRequest.class)
                .map(payload -> new DeviceRegistrationResponse(
                        "reg-" + UUID.randomUUID().toString().substring(0, 8),
                        "DUAL_HOMED_REGISTERED",
                        List.of(
                                "MAC " + payload.macAddress() + " pinned to fabric identity",
                                "Internet IP " + payload.internetIpAddress() + " retained for uplink",
                                "Fabric IP " + payload.fabricIpAddress() + " assigned for control-plane traffic"
                        )
                ))
                .flatMap(response -> ServerResponse.status(CREATED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> hardwareOverview(ServerRequest request) {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        File root = new File("/");

        double systemCpuLoad = systemCpuLoad(baseOsBean);
        long totalMemoryBytes = totalPhysicalMemoryBytes(baseOsBean, runtime.maxMemory());
        long usedMemoryBytes = usedPhysicalMemoryBytes(baseOsBean, totalMemoryBytes, runtime.totalMemory() - runtime.freeMemory());
        long diskTotalBytes = root.getTotalSpace();
        long diskUsedBytes = Math.max(0, diskTotalBytes - root.getUsableSpace());
        int availableProcessors = runtime.availableProcessors();
        long uptimeSeconds = runtimeMxBean.getUptime() / 1_000;
        boolean containerized = new File("/.dockerenv").exists() || System.getenv("KUBERNETES_SERVICE_HOST") != null;
        double heapUsedPercent = percent(heap.getUsed(), heap.getMax() > 0 ? heap.getMax() : heap.getCommitted());
        double memoryUsedPercent = percent(usedMemoryBytes, totalMemoryBytes);
        double diskUsedPercent = percent(diskUsedBytes, diskTotalBytes);
        int loadedClasses = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        long totalStartedThreads = threadMxBean.getTotalStartedThreadCount();

        HardwareOverview overview = new HardwareOverview(
                "local-physical-host",
                Map.of(
                        "cores", availableProcessors,
                        "loadPercent", round(systemCpuLoad),
                        "systemLoadAverage", round(baseOsBean.getSystemLoadAverage()),
                        "thermalState", systemCpuLoad > 85 ? "HOT" : systemCpuLoad > 70 ? "ELEVATED" : "NOMINAL"
                ),
                Map.of(
                        "totalGb", round(gib(totalMemoryBytes)),
                        "usedGb", round(gib(usedMemoryBytes)),
                        "freeGb", round(gib(Math.max(0, totalMemoryBytes - usedMemoryBytes))),
                        "usedPercent", round(memoryUsedPercent),
                        "pressure", memoryUsedPercent > 85 ? "HIGH" : memoryUsedPercent > 70 ? "WATCH" : "LOW"
                ),
                Map.of(
                        "vmPoolGb", round(gib(diskTotalBytes)),
                        "usedGb", round(gib(diskUsedBytes)),
                        "freeGb", round(gib(root.getUsableSpace())),
                        "usedPercent", round(diskUsedPercent),
                        "iopsState", diskUsedPercent > 90 ? "PRESSURE" : "HEALTHY"
                ),
                Map.of("fabricInterfaces", 2, "internetUplink", "ACTIVE", "fabricBridge", "ACTIVE"),
                Map.of(
                        "status", healthStatus(systemCpuLoad, memoryUsedPercent, heapUsedPercent),
                        "db", "UP",
                        "r2dbc", "UP",
                        "ollama", "LOCAL_OR_FALLBACK_READY",
                        "sse", "UP",
                        "metricsSource", containerized ? "CONTAINER_RUNTIME" : "PHYSICAL_HOST_PROCESS",
                        "physicalHostNote", containerized
                                ? "Docker Desktop exposes container runtime metrics. Run backend on the host or attach a host metrics agent for full physical macOS telemetry."
                                : "Backend process is reading live operating-system and JVM metrics from the physical host.",
                        "checkedAt", Instant.now().toString()
                ),
                Map.of(
                        "pid", ProcessHandle.current().pid(),
                        "uptimeSeconds", uptimeSeconds,
                        "startTime", Instant.ofEpochMilli(runtimeMxBean.getStartTime()).toString(),
                        "inputArguments", runtimeMxBean.getInputArguments(),
                        "availableProcessors", availableProcessors
                ),
                Map.of(
                        "live", threadMxBean.getThreadCount(),
                        "daemon", threadMxBean.getDaemonThreadCount(),
                        "peak", threadMxBean.getPeakThreadCount(),
                        "totalStarted", totalStartedThreads
                ),
                Map.of(
                        "name", runtimeMxBean.getVmName(),
                        "vendor", runtimeMxBean.getVmVendor(),
                        "version", runtimeMxBean.getVmVersion(),
                        "heapUsedMb", round(mib(heap.getUsed())),
                        "heapCommittedMb", round(mib(heap.getCommitted())),
                        "heapMaxMb", round(mib(heap.getMax())),
                        "heapUsedPercent", round(heapUsedPercent),
                        "nonHeapUsedMb", round(mib(nonHeap.getUsed())),
                        "loadedClasses", loadedClasses,
                        "gcCollectors", ManagementFactory.getGarbageCollectorMXBeans().stream().map(gc -> gc.getName()).toList()
                ),
                Map.of(
                        "providerMode", "VMWARE_OR_QEMU_GUARDED",
                        "localExecutionEnabled", false,
                        "activeVms", 0,
                        "plannedVms", 1,
                        "capacityState", diskUsedPercent > 90 || memoryUsedPercent > 85 ? "CONSTRAINED" : "READY"
                ),
                Map.of(
                        "cpuPercent", round(systemCpuLoad),
                        "memoryPercent", round(memoryUsedPercent),
                        "heapPercent", round(heapUsedPercent),
                        "diskPercent", round(diskUsedPercent),
                        "threadCount", threadMxBean.getThreadCount()
                ),
                List.of(
                        "Pin AI VMs to the host VM pool and reserve 6 GB memory per local model runtime.",
                        "Quantum systems should stay VM-only and use simulated QPU acceleration until a hardware backend is registered.",
                        "Remote node server promotion should run eligibility checks before installing hypervisor services.",
                        "Watch heap and live thread trends before enabling real VM execution on the physical host."
                ),
                diagnosticAlerts(systemCpuLoad, memoryUsedPercent, heapUsedPercent, diskUsedPercent, threadMxBean.getThreadCount())
        );
        return ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(overview);
    }

    private static double systemCpuLoad(java.lang.management.OperatingSystemMXBean osBean) {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double load = sunOsBean.getCpuLoad();
            return load >= 0 ? load * 100 : 0;
        }
        double loadAverage = osBean.getSystemLoadAverage();
        return loadAverage >= 0 ? Math.min(100, (loadAverage / Math.max(1, osBean.getAvailableProcessors())) * 100) : 0;
    }

    private static long totalPhysicalMemoryBytes(java.lang.management.OperatingSystemMXBean osBean, long fallbackBytes) {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            return sunOsBean.getTotalMemorySize();
        }
        return fallbackBytes;
    }

    private static long usedPhysicalMemoryBytes(java.lang.management.OperatingSystemMXBean osBean, long totalBytes, long fallbackBytes) {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            return Math.max(0, totalBytes - sunOsBean.getFreeMemorySize());
        }
        return fallbackBytes;
    }

    private static String healthStatus(double cpuPercent, double memoryPercent, double heapPercent) {
        if (cpuPercent > 90 || memoryPercent > 90 || heapPercent > 90) {
            return "CRITICAL";
        }
        if (cpuPercent > 75 || memoryPercent > 80 || heapPercent > 80) {
            return "DEGRADED";
        }
        return "UP";
    }

    private static List<String> diagnosticAlerts(double cpuPercent, double memoryPercent, double heapPercent, double diskPercent, int threadCount) {
        List<String> alerts = new java.util.ArrayList<>();
        if (cpuPercent > 75) alerts.add("CPU pressure is elevated; delay heavy AI remediation or VM boot storms.");
        if (memoryPercent > 80) alerts.add("System memory pressure is high; reduce VM memory reservations.");
        if (heapPercent > 80) alerts.add("JVM heap usage is high; inspect reactive pipelines and object retention.");
        if (diskPercent > 85) alerts.add("VM pool storage is nearing capacity; clean old disks before provisioning.");
        if (threadCount > 180) alerts.add("Thread count is elevated; verify no blocking adapters are growing pools.");
        if (alerts.isEmpty()) alerts.add("Runtime health is nominal; no immediate 360-view alerts.");
        return alerts;
    }

    private static double gib(long bytes) {
        return bytes <= 0 ? 0 : bytes / 1_073_741_824.0;
    }

    private static double mib(long bytes) {
        return bytes <= 0 ? 0 : bytes / 1_048_576.0;
    }

    private static double percent(long used, long total) {
        return total <= 0 ? 0 : (used * 100.0) / total;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public Mono<ServerResponse> localVmProviders(ServerRequest request) {
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(localVmService.providers(), Object.class);
    }

    public Mono<ServerResponse> createLocalVm(ServerRequest request) {
        return request.bodyToMono(LocalVmCreateRequest.class)
                .flatMap(localVmService::create)
                .flatMap(response -> ServerResponse.status(ACCEPTED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> aiGuide(ServerRequest request) {
        return request.bodyToMono(AiGuidePrompt.class)
                .map(prompt -> new AiGuideResponse(
                        "guide-" + UUID.randomUUID().toString().substring(0, 8),
                        prompt.persona() == null ? "Aarohi" : prompt.persona(),
                        prompt.language() == null ? "auto" : prompt.language(),
                        aiGuideText(prompt),
                        List.of(
                                new AgentActionProposal(
                                        "open-packet-canvas",
                                        "Open Packet Canvas",
                                        "Open the VueFlow designer so I can help you wire devices.",
                                        "OPEN_TAB",
                                        Map.of("target", "packet-tracing")
                                ),
                                new AgentActionProposal(
                                        "review-360",
                                        "Review 360 View",
                                        "Open diagnostics before changing infrastructure state.",
                                        "OPEN_TAB",
                                        Map.of("target", "360-view")
                                ),
                                new AgentActionProposal(
                                        "open-approval-dashboard",
                                        "Open Approval Dashboard",
                                        "Review CEO-agent recommendations with founder/owner authority before execution.",
                                        "OPEN_TAB",
                                        Map.of("target", "approvals")
                                ),
                                new AgentActionProposal(
                                        "prepare-ai-vm",
                                        "Prepare AI VM Form",
                                        "Switch provisioning to AI VM on the local physical host pool.",
                                        "SET_PROVISIONING_ROLE",
                                        Map.of("targetRoleClass", "AI_VM", "placementMode", "PHYSICAL_HOST_VM")
                                )
                        )
                ))
                .flatMap(response -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> firewallRules(ServerRequest request) {
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(Flux.fromIterable(firewallRules), FirewallRuleEntity.class);
    }

    public Mono<ServerResponse> createFirewallRule(ServerRequest request) {
        return request.bodyToMono(FirewallRuleEntity.class)
                .doOnNext(firewallRules::add)
                .doOnNext(rule -> eventBus.publish(new FabricEvent(
                        UUID.randomUUID().toString(),
                        SECURITY_ALERT,
                        "DROP".equals(rule.policyAction()) ? CRITICAL : INFO,
                        "fabric-firewall-acl",
                        "DROP".equals(rule.policyAction()) ? REMEDIATING : DEGRADED,
                        "Firewall rule committed for " + rule.sourceCidrBlock() + " -> " + rule.networkDestinationPort(),
                        "Validate packet counters and policy convergence.",
                        Instant.now()
                )))
                .flatMap(rule -> ServerResponse.status(CREATED).contentType(APPLICATION_JSON).bodyValue(rule));
    }

    public Mono<ServerResponse> honeypotIncidents(ServerRequest request) {
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(Flux.fromIterable(honeypotIncidents), HoneypotIncidentRecord.class);
    }

    public Mono<ServerResponse> tracePacket(ServerRequest request) {
        return request.bodyToMono(PacketTraceRequest.class)
                .map(trace -> new PacketTraceResult(
                        List.of(
                                new ResolvedHop(1, trace.sourcePointNode() + " encapsulated " + trace.protocolDialectType()),
                                new ResolvedHop(2, "Autonomous Gateway Bridge Transit Node AS-65001 evaluated route vector"),
                                new ResolvedHop(3, "Destination " + trace.destinationIpNode() + " reached with tag " + trace.taggingLayerParam())
                        ),
                        "0000 00 11 22 33 44 55 66 77 88 99 aa bb cc dd ee ff\n0010 08 00 45 00 00 3c 1a 2b 40 00 40 01 af 12 0a c2"
                ))
                .flatMap(result -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(result));
    }

    public Mono<ServerResponse> stageFile(ServerRequest request) {
        return request.bodyToMono(StagedFileMetadata.class)
                .map(metadata -> new StagedAssetResponse("file-" + Math.abs(metadata.assetName().hashCode())))
                .flatMap(response -> ServerResponse.status(CREATED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> executePipeline(ServerRequest request) {
        return request.bodyToMono(PipelineRequest.class)
                .map(pipeline -> {
                    String widgetId = "pipe-" + UUID.randomUUID().toString().substring(0, 8);
                    eventBus.publish(new FabricEvent(
                            UUID.randomUUID().toString(),
                            PIPELINE_UPDATE,
                            INFO,
                            pipeline.deploymentDestinationHost(),
                            REMEDIATING,
                            "Asynchronous payload stream started for " + pipeline.targetAssetToken(),
                            "Monitor pipe widget " + widgetId,
                            Instant.now()
                    ));
                    return new PipelineResponse(widgetId, "RUNNING");
                })
                .flatMap(response -> ServerResponse.status(ACCEPTED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> terminalCommand(ServerRequest request) {
        return request.bodyToMono(TelnetCommandPayload.class)
                .map(payload -> new TelnetCommandResponse(commandOutput(payload)))
                .flatMap(response -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> executeQuantumCircuit(ServerRequest request) {
        return request.bodyToMono(QuantumCircuitExecutionRequest.class)
                .map(payload -> new ComputeJobStatus(
                        "job-qasm-" + UUID.randomUUID().toString().substring(0, 8),
                        "RUNNING",
                        Map.of(
                                "quantumMetrics", Map.of("quantumVolume", 2048, "fidelityRatio", 0.99842),
                                "target", payload.physicalTargetQpu()
                        )
                ))
                .flatMap(response -> ServerResponse.status(ACCEPTED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> spawnSocket(ServerRequest request) {
        return request.bodyToMono(ServerSpawnRequest.class)
                .map(payload -> new ServerInstanceRecord("sock-" + payload.localPort() + "-active", "BOUND_LISTENING"))
                .flatMap(response -> ServerResponse.status(CREATED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> scrambleIdentity(ServerRequest request) {
        return request.bodyToMono(IdentityScrambleIntent.class)
                .map(ignored -> new ScrambleExecutionSummary("SUCCESS", 3))
                .doOnNext(summary -> eventBus.publish(new FabricEvent(
                        UUID.randomUUID().toString(),
                        NODE_STATUS,
                        INFO,
                        "edge-ingress-router-r1",
                        REMEDIATING,
                        "Network coordinate scramble committed across " + summary.affectedInterfacesCount() + " interfaces.",
                        "Refresh inventory IP assignments and session bindings.",
                        Instant.now()
                )))
                .flatMap(response -> ServerResponse.status(ACCEPTED).contentType(APPLICATION_JSON).bodyValue(response));
    }

    private String commandOutput(TelnetCommandPayload payload) {
        String command = payload.rawCommandText() == null ? "" : payload.rawCommandText().toLowerCase();
        return switch (command) {
            case "show interface brief" -> "Interface IP-Address OK? Method Status Protocol\nEthernet0/0 "
                    + payload.targetedHostEndpoint() + " YES manual up up\nLoopback0 127.0.0.1 YES manual up up";
            case "show runtime architecture" -> "Fabric Control Plane Hypervisor: QEMU Bare-Metal Cluster Node Layer\nTransport: "
                    + payload.protocolDialectMode();
            default -> "% Parsed command '" + payload.rawCommandText() + "' accepted by sandbox terminal loop";
        };
    }

    private List<String> provisioningActions(ServerProvisionIntent intent, String placement) {
        if ("QUANTUM_VM".equals(intent.targetRoleClass())) {
            return List.of(
                    "Force deploymentFramework=VIRTUAL_MACHINE for quantum runtime isolation.",
                    "Attach simulated QPU profile before exposing quantum circuit endpoint."
            );
        }
        if ("REMOTE_NODE".equals(placement)) {
            return List.of(
                    "Run eligibility script on " + intent.parentNodeId(),
                    "Verify CPU virtualization, memory pressure, disk pool, and dual-network reachability.",
                    "If checks pass, run install script to promote node as server host; otherwise return remediation steps."
            );
        }
        return List.of(
                "Create VM on the physical system where Fabric Engine is running.",
                "Attach internet uplink and fabric bridge for dual-IP control.",
                "Register MAC identity and expose hardware telemetry in 360 view."
        );
    }

    private String aiGuideText(AiGuidePrompt prompt) {
        String persona = prompt.persona() == null ? "Aarohi" : prompt.persona();
        String language = prompt.language() == null ? "auto" : prompt.language();
        String message = prompt.message() == null ? "" : prompt.message();
        if ("hi-IN".equals(language) || message.matches(".*[\\u0900-\\u097F].*")) {
            return persona + " bol rahi hoon. Main Reactor ki CEO-agent hoon: product direction, risk priority, aur operating plan main sambhaalungi. "
                    + "Lekin aap founder/owner ho, final approval hamesha aapka rahega. Maine aapki baat samajh li: " + message
                    + ". Main pehle simple Hindi-Hinglish mein plan bataungi, phir approval dashboard mein decision proposal dungi. "
                    + "Aapke approve kiye bina main VM, server, packet route, ya security policy mein koi change nahi karungi. "
                    + "Abhi safest next step hai 360 View mein CPU, memory, JVM heap, threads aur VM pool pressure dekhna, "
                    + "phir jis node par kaam karna hai uska exact naam bol dijiye.";
        }
        if ("en-IN".equals(language) || "auto".equals(language)) {
            return persona + " here. I am operating as Reactor CEO-agent: I can shape product priorities, assess risk, and prepare decisions. "
                    + "You remain founder/owner, so final authority stays with you. I understood your request: " + message
                    + ". I will guide you step by step in natural Indian English or Hinglish, keep the risk visible, "
                    + "and move every platform-changing action into the approval dashboard before execution. For this request, I would first check 360 health, "
                    + "confirm the target node or VM, then prepare the action for your approval.";
        }
        return persona + " here. I understood your request: " + message
                + ". I will act as Reactor CEO-agent, explain the plan clearly, keep founder/owner approval as the final gate, and prepare safe next actions only after you confirm.";
    }
}
