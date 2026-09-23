package com.trustabac.iot.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer for unprocessable or poisoned messages routed to the Dead Letter Queue (DLQ).
 */
@Component
@ConditionalOnProperty(prefix = "trustabac.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    private final AtomicLong dlqMessageCount = new AtomicLong(0);

    @RabbitListener(queues = "${trustabac.messaging.dlq-queue:trustabac.dlq}")
    public void handleDeadLetterMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.warn("Received dead-lettered message from DLQ: {} (Headers: {})",
                body, message.getMessageProperties().getHeaders());

        dlqMessageCount.incrementAndGet();
    }

    public long getDlqMessageCount() {
        return dlqMessageCount.get();
    }

    public void resetMetrics() {
        dlqMessageCount.set(0);
    }
}
