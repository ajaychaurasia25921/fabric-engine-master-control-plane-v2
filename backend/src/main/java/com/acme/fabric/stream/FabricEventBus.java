package com.acme.fabric.stream;

import java.time.Duration;

import com.acme.fabric.domain.FabricModels.FabricEvent;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class FabricEventBus {
    private final Sinks.Many<FabricEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<FabricEvent> events() {
        return sink.asFlux();
    }

    public FabricEvent publish(FabricEvent event) {
        sink.emitNext(event, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(250)));
        return event;
    }
}
