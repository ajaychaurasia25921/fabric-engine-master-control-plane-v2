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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.acme.fabric.domain.FabricModels.AllocationMetadata;
import com.acme.fabric.domain.FabricModels.AiGuidePrompt;
import com.acme.fabric.domain.FabricModels.AiGuideResponse;
import com.acme.fabric.domain.FabricModels.ComputeJobStatus;
import com.acme.fabric.domain.FabricModels.DeviceRegistrationRequest;
import com.acme.fabric.domain.FabricModels.DeviceRegistrationResponse;
import com.acme.fabric.domain.FabricModels.FabricEvent;
import com.acme.fabric.domain.FabricModels.FirewallRuleEntity;
import com.acme.fabric.domain.FabricModels.HardwareOverview;
import com.acme.fabric.domain.FabricModels.HoneypotIncidentRecord;
import com.acme.fabric.domain.FabricModels.IdentityScrambleIntent;
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
import com.acme.fabric.stream.FabricEventBus;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ControlPlaneHandler {
    private final FabricEventBus eventBus;
    private final CopyOnWriteArrayList<FirewallRuleEntity> firewallRules = new CopyOnWriteArrayList<>(List.of(
            new FirewallRuleEntity("10.0.0.0/8", "23/TCP", "DROP"),
            new FirewallRuleEntity("192.168.1.0/24", "22/TCP", "ACCEPT")
    ));
    private final CopyOnWriteArrayList<HoneypotIncidentRecord> honeypotIncidents = new CopyOnWriteArrayList<>(List.of(
            new HoneypotIncidentRecord(UUID.randomUUID().toString(), "185.220.101.4", "Legacy Telnet Root Prompt", 23, Instant.now())
    ));

    public ControlPlaneHandler(FabricEventBus eventBus) {
        this.eventBus = eventBus;
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
        HardwareOverview overview = new HardwareOverview(
                "local-physical-host",
                Map.of("cores", 8, "loadPercent", 42, "thermalState", "NOMINAL"),
                Map.of("totalGb", 32, "usedGb", 18, "pressure", "LOW"),
                Map.of("vmPoolGb", 512, "usedGb", 226, "iopsState", "HEALTHY"),
                Map.of("fabricInterfaces", 2, "internetUplink", "ACTIVE", "fabricBridge", "ACTIVE"),
                List.of(
                        "Pin AI VMs to the host VM pool and reserve 6 GB memory per local model runtime.",
                        "Quantum systems should stay VM-only and use simulated QPU acceleration until a hardware backend is registered.",
                        "Remote node server promotion should run eligibility checks before installing hypervisor services."
                ),
                List.of("Ollama CPU inference can spike during remediation; keep rule-based fallback enabled.")
        );
        return ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(overview);
    }

    public Mono<ServerResponse> aiGuide(ServerRequest request) {
        return request.bodyToMono(AiGuidePrompt.class)
                .map(prompt -> new AiGuideResponse(
                        "guide-" + UUID.randomUUID().toString().substring(0, 8),
                        "Fabric Companion",
                        "I can help wire devices, register MAC identities, decide whether to provision on the physical host or a remote node, and explain alerts. For this request: "
                                + prompt.message(),
                        List.of("Open Packet Canvas", "Review Hardware 360", "Run Node Eligibility Check")
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
}
