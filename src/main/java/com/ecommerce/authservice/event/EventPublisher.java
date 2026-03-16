package com.ecommerce.authservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NO-OP EventPublisher — active while Kafka is disabled (REST mode).
 *
 * <p>All publish calls are silently swallowed and logged at DEBUG level
 * so that the rest of the codebase compiles and runs without changes.
 *
 * <p>To re-enable Kafka:
 * <ol>
 *   <li>Restore KafkaProducerConfig, KafkaConsumerConfig, KafkaTopicConfig.</li>
 *   <li>Replace this file with the original KafkaTemplate-backed implementation.</li>
 * </ol>
 */
@Slf4j
@Component
public class EventPublisher {

    /**
     * No-op — event is logged and discarded.
     * In Kafka mode this sends the event to the given topic immediately.
     */
    public void publish(String topic, Object event) {
        log.debug("[REST-MODE] Skipping Kafka publish — topic={} eventType={}",
                topic, event.getClass().getSimpleName());
    }

    /**
     * No-op — event is logged and discarded.
     * In Kafka mode this sends the event after the current transaction commits.
     */
    public void publishAfterCommit(String topic, Object event) {
        log.debug("[REST-MODE] Skipping Kafka publishAfterCommit — topic={} eventType={}",
                topic, event.getClass().getSimpleName());
    }
}
