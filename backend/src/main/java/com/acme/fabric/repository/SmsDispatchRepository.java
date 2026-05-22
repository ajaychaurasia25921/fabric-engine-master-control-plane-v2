package com.acme.fabric.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SmsDispatchRepository extends ReactiveCrudRepository<SmsDispatchEntity, String> {
}
