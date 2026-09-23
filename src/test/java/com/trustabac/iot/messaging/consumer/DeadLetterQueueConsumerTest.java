package com.trustabac.iot.messaging.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeadLetterQueueConsumerTest {

    private DeadLetterQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DeadLetterQueueConsumer();
    }

    @Test
    @DisplayName("Test DeadLetterQueueConsumer receives and counts dead-lettered message")
    void testHandleDeadLetterMessage() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-death-reason", "rejected");
        Message message = new Message("{\"invalid\": \"payload\"}".getBytes(StandardCharsets.UTF_8), properties);

        consumer.handleDeadLetterMessage(message);

        assertEquals(1, consumer.getDlqMessageCount());
    }
}
