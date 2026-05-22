package com.acme.fabric.repository;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sms_dispatches")
public record SmsDispatchEntity(
        @Id String id,
        @Column("target_mobile_endpoint") String targetMobileEndpoint,
        @Column("sms_message_text") String smsMessageText,
        @Column("signaling_protocol") String signalingProtocol,
        @Column("provider_message_id") String providerMessageId,
        @Column("gateway_status") String gatewayStatus,
        @Column("submitted_at") Instant submittedAt
) {
}
