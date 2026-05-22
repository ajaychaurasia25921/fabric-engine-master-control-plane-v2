package com.acme.fabric.handler;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

import com.acme.fabric.domain.FabricModels.FabricEvent;
import com.acme.fabric.stream.FabricEventBus;
import com.acme.fabric.stream.FabricTopologyService;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class FabricHandler {
    private final FabricTopologyService topologyService;
    private final FabricEventBus eventBus;

    public FabricHandler(FabricTopologyService topologyService, FabricEventBus eventBus) {
        this.topologyService = topologyService;
        this.eventBus = eventBus;
    }

    public Mono<ServerResponse> topology(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .body(topologyService.current(), Object.class);
    }

    public Mono<ServerResponse> publishEvent(ServerRequest request) {
        return request.bodyToMono(FabricEvent.class)
                .map(eventBus::publish)
                .flatMap(event -> ServerResponse.accepted()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(event));
    }

    public Mono<ServerResponse> stream(ServerRequest request) {
        var sse = eventBus.events()
                .map(event -> ServerSentEvent.builder(event)
                        .id(event.id())
                        .event(event.type().name())
                        .build());

        return ServerResponse.ok()
                .contentType(TEXT_EVENT_STREAM)
                .body(sse, ServerSentEvent.class);
    }
}
