package com.acme.fabric.sms;

import com.acme.fabric.domain.FabricModels.SmsDispatchPayload;

import reactor.core.publisher.Mono;

public interface SmppClient {
    Mono<String> submit(SmsDispatchPayload request);
}
