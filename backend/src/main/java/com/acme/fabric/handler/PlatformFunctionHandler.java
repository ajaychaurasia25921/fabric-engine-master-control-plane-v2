package com.acme.fabric.handler;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.acme.fabric.domain.FabricModels.PolyglotFunctionBlueprintRequest;
import com.acme.fabric.domain.FabricModels.PolyglotScriptRequest;
import com.acme.fabric.provisioning.PolyglotFunctionFactoryService;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class PlatformFunctionHandler {
    private final PolyglotFunctionFactoryService factoryService;
    private final Validator validator;

    public PlatformFunctionHandler(PolyglotFunctionFactoryService factoryService, Validator validator) {
        this.factoryService = factoryService;
        this.validator = validator;
    }

    public Mono<ServerResponse> createBlueprint(ServerRequest request) {
        return request.bodyToMono(PolyglotFunctionBlueprintRequest.class)
                .flatMap(this::validate)
                .flatMap(factoryService::generateBlueprint)
                .flatMap(response -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(response));
    }

    public Mono<ServerResponse> evaluateScript(ServerRequest request) {
        return request.bodyToMono(PolyglotScriptRequest.class)
                .flatMap(this::validate)
                .flatMap(factoryService::evaluate)
                .flatMap(response -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(response));
    }

    private <T> Mono<T> validate(T request) {
        var violations = validator.validate(request);
        if (violations.isEmpty()) {
            return Mono.just(request);
        }
        return Mono.error(new IllegalArgumentException(violations.iterator().next().getMessage()));
    }
}
