package com.acme.fabric.stream;

import static com.acme.fabric.domain.FabricModels.FabricEventType.NODE_STATUS;
import static com.acme.fabric.domain.FabricModels.FabricEventType.SECURITY_ALERT;
import static com.acme.fabric.domain.FabricModels.NodeStatus.DEGRADED;
import static com.acme.fabric.domain.FabricModels.NodeStatus.DOWN;
import static com.acme.fabric.domain.FabricModels.NodeStatus.REMEDIATING;
import static com.acme.fabric.domain.FabricModels.NodeStatus.RUNNING;
import static com.acme.fabric.domain.FabricModels.Severity.CRITICAL;
import static com.acme.fabric.domain.FabricModels.Severity.INFO;
import static com.acme.fabric.domain.FabricModels.Severity.WARNING;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.acme.fabric.domain.FabricModels.FabricEvent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Component
public class FabricTelemetrySimulator {
    private final FabricEventBus eventBus;
    private final boolean enabled;
    private Disposable subscription;

    public FabricTelemetrySimulator(FabricEventBus eventBus,
                                    @Value("${fabric.telemetry.demo-events-enabled:false}") boolean enabled) {
        this.eventBus = eventBus;
        this.enabled = enabled;
    }

    @PostConstruct
    void start() {
        if (!enabled) {
            return;
        }
        subscription = Flux.interval(Duration.ofSeconds(8))
                .map(this::demoEvent)
                .subscribe(eventBus::publish);
    }

    @PreDestroy
    void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    private FabricEvent demoEvent(long tick) {
        return switch ((int) (tick % 4)) {
            case 0 -> event("edge-ingress-router-r1", NODE_STATUS, WARNING, DEGRADED, "Packet loss crossed 2 percent.", "Watch queue depth and ECMP balance.");
            case 1 -> event("honeypot-trap-router-vty", SECURITY_ALERT, CRITICAL, DOWN, "Lateral movement signature detected.", "Trigger containment policy.");
            case 2 -> event("l7-socket-gateway", NODE_STATUS, INFO, RUNNING, "Service mesh latency recovered.", "No action required.");
            default -> event("fabric-firewall-acl", NODE_STATUS, CRITICAL, REMEDIATING, "Firewall policy reload in progress.", "Track ACL commit convergence.");
        };
    }

    private FabricEvent event(String nodeId, com.acme.fabric.domain.FabricModels.FabricEventType type,
                              com.acme.fabric.domain.FabricModels.Severity severity,
                              com.acme.fabric.domain.FabricModels.NodeStatus status,
                              String message, String recommendation) {
        return new FabricEvent(UUID.randomUUID().toString(), type, severity, nodeId, status, message, recommendation, Instant.now());
    }
}
