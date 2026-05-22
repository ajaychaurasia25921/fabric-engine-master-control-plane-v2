package com.acme.fabric.agent;

import static com.acme.fabric.domain.FabricModels.FabricEventType.AI_REMEDIATION;
import static com.acme.fabric.domain.FabricModels.FabricEventType.SECURITY_ALERT;
import static com.acme.fabric.domain.FabricModels.NodeStatus.DOWN;
import static com.acme.fabric.domain.FabricModels.NodeStatus.REMEDIATING;
import static com.acme.fabric.domain.FabricModels.Severity.CRITICAL;
import static com.acme.fabric.domain.FabricModels.Severity.INFO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.acme.fabric.domain.FabricModels.FabricEvent;
import com.acme.fabric.stream.FabricEventBus;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class FabricAgent {
    private static final Logger log = LoggerFactory.getLogger(FabricAgent.class);
    private static final int RAG_CHUNK_SIZE = 3_000;

    private final FabricEventBus eventBus;
    private final Optional<VectorStore> vectorStore;
    private final Optional<ChatClient> chatClient;
    private final Path openApiPath;
    private Disposable subscription;

    public FabricAgent(
            FabricEventBus eventBus,
            ObjectProvider<VectorStore> vectorStore,
            ObjectProvider<ChatClient.Builder> chatClientBuilder,
            @Value("${fabric.openapi-path}") String openApiPath
    ) {
        this.eventBus = eventBus;
        this.vectorStore = Optional.ofNullable(vectorStore.getIfAvailable());
        this.chatClient = Optional.ofNullable(chatClientBuilder.getIfAvailable()).map(ChatClient.Builder::build);
        this.openApiPath = Path.of(openApiPath);
    }

    @PostConstruct
    void start() {
        ingestOpenApiContext()
                .then(Mono.fromRunnable(() -> subscription = eventBus.events()
                        .filter(this::isAnomaly)
                        .flatMap(this::remediate)
                        .onErrorContinue((error, event) -> log.warn("Remediation stream skipped event {}: {}", event, error.getMessage()))
                        .subscribe(eventBus::publish, error -> log.warn("Remediation stream stopped: {}", error.getMessage()))))
                .subscribe(null, error -> log.warn("Fabric agent startup skipped: {}", error.getMessage()));
    }

    @PreDestroy
    void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    public Mono<FabricEvent> remediate(FabricEvent anomaly) {
        return Mono.delay(Duration.ofMillis(350))
                .flatMap(ignored -> generatePlan(anomaly))
                .map(plan -> new FabricEvent(
                        UUID.randomUUID().toString(),
                        AI_REMEDIATION,
                        INFO,
                        anomaly.nodeId(),
                        REMEDIATING,
                        "Autonomous remediation started for " + anomaly.nodeId(),
                        plan,
                        Instant.now()
                ))
                .onErrorResume(error -> {
                    log.warn("Using rule-based remediation for {}: {}", anomaly.nodeId(), error.getMessage());
                    return Mono.just(new FabricEvent(
                            UUID.randomUUID().toString(),
                            AI_REMEDIATION,
                            INFO,
                            anomaly.nodeId(),
                            REMEDIATING,
                            "Autonomous remediation started for " + anomaly.nodeId(),
                            ruleBasedPlan(anomaly),
                            Instant.now()
                    ));
                });
    }

    private Mono<Void> ingestOpenApiContext() {
        if (vectorStore.isEmpty()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> Files.exists(openApiPath)
                        ? Files.readString(openApiPath)
                        : "OpenAPI contract not found at " + openApiPath)
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::chunkOpenApiSpec)
                .doOnNext(vectorStore.get()::add)
                .doOnError(error -> log.warn("OpenAPI RAG ingestion skipped: {}", error.getMessage()))
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    private List<Document> chunkOpenApiSpec(String spec) {
        List<Document> documents = new ArrayList<>();
        for (int start = 0; start < spec.length(); start += RAG_CHUNK_SIZE) {
            int end = Math.min(start + RAG_CHUNK_SIZE, spec.length());
            documents.add(new Document(spec.substring(start, end)));
        }
        return documents;
    }

    private Mono<String> generatePlan(FabricEvent anomaly) {
        return chatClient
                .map(client -> Mono.fromCallable(() -> client.prompt()
                        .system("You are the Fabric Engine remediation agent. Use the API contract context and return a concise remediation plan.")
                        .user("Create an infrastructure remediation plan for this event: " + anomaly)
                        .call()
                        .content())
                        .subscribeOn(Schedulers.boundedElastic())
                        .timeout(Duration.ofSeconds(8))
                        .onErrorResume(error -> {
                            log.warn("Ollama remediation plan timed out or failed for {}: {}", anomaly.nodeId(), error.getMessage());
                            return Mono.just(ruleBasedPlan(anomaly));
                        }))
                .orElseGet(() -> Mono.just(ruleBasedPlan(anomaly)));
    }

    private boolean isAnomaly(FabricEvent event) {
        return event.severity() == CRITICAL || event.type() == SECURITY_ALERT || event.status() == DOWN;
    }

    private String ruleBasedPlan(FabricEvent anomaly) {
        return switch (anomaly.type()) {
            case SECURITY_ALERT -> "Quarantine the segment, rotate policy, and increase telemetry sampling.";
            case NODE_STATUS -> "Drain traffic, validate BGP/EVPN adjacency, and restart the node fabric agent.";
            case AI_REMEDIATION -> "Continue observing remediation convergence.";
            case PIPELINE_UPDATE -> "Verify the pipeline checkpoint, hold mutable actions, and continue packet trace telemetry.";
        };
    }
}
