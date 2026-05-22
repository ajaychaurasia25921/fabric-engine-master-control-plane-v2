package com.acme.fabric.handler;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.acme.fabric.domain.FabricModels.SmsDispatchPayload;
import com.acme.fabric.sms.SmsDispatchService;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class SmsDispatchHandler {
    private final SmsDispatchService dispatchService;
    private final Validator validator;

    public SmsDispatchHandler(SmsDispatchService dispatchService, Validator validator) {
        this.dispatchService = dispatchService;
        this.validator = validator;
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(SmsDispatchPayload.class)
                .flatMap(this::validate)
                .flatMap(dispatchService::dispatch)
                .flatMap(response -> ServerResponse.accepted()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(response));
    }

    private Mono<SmsDispatchPayload> validate(SmsDispatchPayload request) {
        var violations = validator.validate(request);
        if (violations.isEmpty()) {
            return Mono.just(request);
        }
        return Mono.error(new IllegalArgumentException(violations.iterator().next().getMessage()));
    }
}
