package com.acme.fabric.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class FabricModels {
    private FabricModels() {
    }

    public record FabricTopology(List<FabricNode> nodes, List<FabricEdge> edges) {
    }

    public record FabricNode(String id, String label, NodeRole role, NodeStatus status, double x, double y) {
    }

    public record FabricEdge(String id, String source, String target, String label) {
    }

    public record FabricEvent(
            String id,
            FabricEventType type,
            Severity severity,
            String nodeId,
            NodeStatus status,
            String message,
            String recommendation,
            Instant occurredAt
    ) {
    }

    public record SmsDispatchPayload(
            @NotBlank String targetMobileEndpoint,
            @NotBlank @Size(max = 160) String smsMessageText,
            @NotNull SignalingProtocol signalingProtocol
    ) {
    }

    public record SmsDispatchResponse(String smsTrackingId, GatewayStatus gatewayStatus) {
    }

    public record SmsGatewayMetrics(String connectedSmsC, int activeModemPorts, int signalStrengthDbm) {
    }

    public record ServerProvisionIntent(
            @NotBlank String serverName,
            @NotBlank String parentNodeId,
            @NotBlank String deploymentFramework,
            @NotBlank String executionScope,
            @NotBlank String targetRoleClass,
            String placementMode,
            String registeredMacAddress,
            String internetIpAddress,
            String fabricIpAddress,
            DbRoleConfig dbRoleConfig,
            AppRoleConfig appRoleConfig,
            HoneypotConfig honeypotConfig,
            AiVmConfig aiVmConfig,
            QuantumVmConfig quantumVmConfig,
            NodeEligibilityConfig nodeEligibilityConfig
    ) {
    }

    public record DbRoleConfig(String databaseEngine, Integer localListeningPort) {
    }

    public record AppRoleConfig(String runtimeEnvironment, Integer targetServicePort) {
    }

    public record HoneypotConfig(String simulatedVulnerabilityPersona, String telemetryAttackIncidentAlertingQueue) {
    }

    public record AiVmConfig(String modelRuntime, String acceleratorProfile, Integer inferencePort) {
    }

    public record QuantumVmConfig(String qpuSimulator, Integer qubitCount, String circuitRuntime) {
    }

    public record NodeEligibilityConfig(String bootstrapEndpoint, String checkScript, String installScript) {
    }

    public record ServerProvisionStatus(String serverId, AllocationMetadata allocationMetadata, RuntimeState runtimeState, List<String> nextActions) {
    }

    public record AllocationMetadata(String assignedClusterIp, String internetIpAddress, String registeredMacAddress, String placementMode, Instant bootstrapTimestamp) {
    }

    public record FirewallRuleEntity(String sourceCidrBlock, String networkDestinationPort, String policyAction) {
    }

    public record HoneypotIncidentRecord(
            String incidentUuid,
            String attackerIpSource,
            String vectorProfile,
            int targetedPort,
            Instant capturedTimestamp
    ) {
    }

    public record PacketTraceRequest(
            String sourcePointNode,
            String destinationIpNode,
            String protocolDialectType,
            String taggingLayerParam
    ) {
    }

    public record PacketTraceResult(List<ResolvedHop> resolvedHopChain, String diagnosticHexDumpChunk) {
    }

    public record ResolvedHop(int hopIndex, String description) {
    }

    public record StagedFileMetadata(String assetName, long assetByteSizeBytes, String mediaMimeType) {
    }

    public record StagedAssetResponse(String stagedAssetToken) {
    }

    public record PipelineRequest(String targetAssetToken, String deploymentDestinationHost) {
    }

    public record PipelineResponse(String pipelineWidgetId, String initialState) {
    }

    public record TelnetCommandPayload(String targetedHostEndpoint, String protocolDialectMode, String rawCommandText) {
    }

    public record TelnetCommandResponse(String stdoutBuffer) {
    }

    public record QuantumCircuitExecutionRequest(
            String physicalTargetQpu,
            Integer sequenceGateDepth,
            String processingAlgorithmClass
    ) {
    }

    public record ComputeJobStatus(String jobId, String executionState, Object outputPayloadMetrics) {
    }

    public record ServerSpawnRequest(Integer localPort, String proxyTarget) {
    }

    public record ServerInstanceRecord(String socketInterfaceId, String bindingState) {
    }

    public record IdentityScrambleIntent(Boolean triggerConfirmation) {
    }

    public record ScrambleExecutionSummary(String randomizationLoopStatus, int affectedInterfacesCount) {
    }

    public record DeviceRegistrationRequest(
            @NotBlank String nodeName,
            @NotBlank String macAddress,
            @NotBlank String internetIpAddress,
            @NotBlank String fabricIpAddress,
            String deviceClass
    ) {
    }

    public record DeviceRegistrationResponse(String registrationId, String networkState, List<String> assignedPolicies) {
    }

    public record HardwareOverview(
            String physicalHost,
            Map<String, Object> cpu,
            Map<String, Object> memory,
            Map<String, Object> storage,
            Map<String, Object> network,
            List<String> aiSuggestions,
            List<String> alerts
    ) {
    }

    public record AiGuidePrompt(@NotBlank String message, Map<String, Object> context) {
    }

    public record AiGuideResponse(String guideId, String persona, String response, List<String> suggestedActions) {
    }

    public record PolyglotFunctionBlueprintRequest(
            @NotBlank String targetPlatform,
            @NotBlank String functionName,
            @NotBlank String language,
            @NotBlank String handlerSource,
            Map<String, Object> environment
    ) {
    }

    public record GeneratedSourceFile(String path, String content) {
    }

    public record PolyglotFunctionBlueprintResponse(
            String artifactId,
            String targetPlatform,
            String runtime,
            List<GeneratedSourceFile> files,
            List<String> deploymentHints
    ) {
    }

    public record PolyglotScriptRequest(
            @NotBlank String language,
            @NotBlank String source,
            Map<String, Object> bindings
    ) {
    }

    public record PolyglotScriptResponse(
            String executionId,
            String language,
            Object result,
            Instant executedAt
    ) {
    }

    public enum NodeRole {
        CCNP_EDGE_CORE, HONEYPOT_DECOY, DATABASE_SERVER, APPLICATION_SERVER, FIREWALL_APPLIANCE, AI_VM, QUANTUM_VM
    }

    public enum NodeStatus {
        RUNNING, PROVISIONED, INITIALIZING, DEGRADED, DOWN, REMEDIATING
    }

    public enum FabricEventType {
        NODE_STATUS, SECURITY_ALERT, AI_REMEDIATION, PIPELINE_UPDATE
    }

    public enum Severity {
        INFO, WARNING, CRITICAL
    }

    public enum SignalingProtocol {
        SMPP_V34, GSM_AT_COMMANDS, REST_FORWARD
    }

    public enum GatewayStatus {
        QUEUED, TRANSMITTING, CARRIER_ACK
    }

    public enum RuntimeState {
        INITIALIZING, RUNNING, DEGRADED, TERMINATED
    }
}
