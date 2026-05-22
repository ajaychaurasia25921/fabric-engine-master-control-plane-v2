package com.acme.fabric.sms;

import java.time.Duration;
import java.util.UUID;

import com.acme.fabric.domain.FabricModels.SmsDispatchPayload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ReactiveSmppClient implements SmppClient {
    private final String host;
    private final int port;

    public ReactiveSmppClient(
            @Value("${fabric.smpp.host}") String host,
            @Value("${fabric.smpp.port}") int port
    ) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Mono<String> submit(SmsDispatchPayload request) {
        return Mono.delay(Duration.ofMillis(120))
                .map(ignored -> "smpp-" + host + "-" + port + "-" + UUID.randomUUID());
    }
}
