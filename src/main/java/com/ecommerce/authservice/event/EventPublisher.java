package com.ecommerce.authservice.event;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CorrelationIdProvider correlationIdProvider;

    public void publish(String topic, Object event) {
        String correlationId = correlationIdProvider.currentOrGenerate();
        long start = System.nanoTime();

        Message<Object> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(CorrelationIdProvider.HEADER_NAME, correlationId)
                .build();

        kafkaTemplate.send(message).whenComplete((result, ex) -> {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            String eventType = event.getClass().getSimpleName();
            if (ex == null) {
                log.info("Event published topic={} eventType={} durationMs={} correlationId={}",
                        topic, eventType, durationMs, correlationId);
            } else {
                log.error("Event publish failed topic={} eventType={} durationMs={} correlationId={}",
                        topic, eventType, durationMs, correlationId, ex);
            }
        });
    }

    public void publishAfterCommit(String topic, Object event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publish(topic, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(topic, event);
            }
        });
    }
}
