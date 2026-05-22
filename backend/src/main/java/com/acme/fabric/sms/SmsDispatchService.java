package com.acme.fabric.sms;

import static com.acme.fabric.domain.FabricModels.GatewayStatus.QUEUED;

import java.time.Instant;
import java.util.UUID;

import com.acme.fabric.domain.FabricModels.SmsDispatchPayload;
import com.acme.fabric.domain.FabricModels.SmsDispatchResponse;
import com.acme.fabric.repository.SmsDispatchEntity;
import com.acme.fabric.repository.SmsDispatchRepository;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SmsDispatchService {
    private final SmppClient smppClient;
    private final SmsDispatchRepository repository;

    public SmsDispatchService(SmppClient smppClient, SmsDispatchRepository repository) {
        this.smppClient = smppClient;
        this.repository = repository;
    }

    public Mono<SmsDispatchResponse> dispatch(SmsDispatchPayload request) {
        String id = UUID.randomUUID().toString();
        Instant submittedAt = Instant.now();
        return smppClient.submit(request)
                .flatMap(providerMessageId -> repository.save(new SmsDispatchEntity(
                        id,
                        request.targetMobileEndpoint(),
                        request.smsMessageText(),
                        request.signalingProtocol().name(),
                        providerMessageId,
                        QUEUED.name(),
                        submittedAt
                )))
                .map(entity -> new SmsDispatchResponse(entity.id(), QUEUED));
    }
}
