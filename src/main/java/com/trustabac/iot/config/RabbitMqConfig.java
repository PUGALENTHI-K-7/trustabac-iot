package com.trustabac.iot.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AMQP RabbitMQ topology configuration.
 * Declares exchanges, queues with dead-letter bindings, and JSON serialization.
 */
@Configuration
@ConditionalOnProperty(prefix = "trustabac.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    private final MessagingProperties properties;

    public RabbitMqConfig(MessagingProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TopicExchange trustabacExchange() {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(properties.getDlqQueue()).build();
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, TopicExchange trustabacExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(trustabacExchange)
                .with(properties.getRoutingKeyDlq());
    }

    @Bean
    public Queue deviceEventQueue() {
        return QueueBuilder.durable(properties.getDeviceEventQueue())
                .withArgument("x-dead-letter-exchange", properties.getExchange())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeyDlq())
                .build();
    }

    @Bean
    public Binding deviceEventBinding(Queue deviceEventQueue, TopicExchange trustabacExchange) {
        return BindingBuilder.bind(deviceEventQueue)
                .to(trustabacExchange)
                .with(properties.getRoutingKeyDeviceOperation());
    }

    @Bean
    public Queue trustEventQueue() {
        return QueueBuilder.durable(properties.getTrustEventQueue())
                .withArgument("x-dead-letter-exchange", properties.getExchange())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeyDlq())
                .build();
    }

    @Bean
    public Binding trustEventBinding(Queue trustEventQueue, TopicExchange trustabacExchange) {
        return BindingBuilder.bind(trustEventQueue)
                .to(trustabacExchange)
                .with(properties.getRoutingKeyTrustEvent());
    }

    @Bean
    public Queue riskEventQueue() {
        return QueueBuilder.durable(properties.getRiskEventQueue())
                .withArgument("x-dead-letter-exchange", properties.getExchange())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeyDlq())
                .build();
    }

    @Bean
    public Binding riskEventBinding(Queue riskEventQueue, TopicExchange trustabacExchange) {
        return BindingBuilder.bind(riskEventQueue)
                .to(trustabacExchange)
                .with(properties.getRoutingKeyRiskEvent());
    }

    @Bean
    public Queue authorizationEventQueue() {
        return QueueBuilder.durable(properties.getAuthorizationEventQueue())
                .withArgument("x-dead-letter-exchange", properties.getExchange())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeyDlq())
                .build();
    }

    @Bean
    public Binding authorizationEventBinding(Queue authorizationEventQueue, TopicExchange trustabacExchange) {
        return BindingBuilder.bind(authorizationEventQueue)
                .to(trustabacExchange)
                .with(properties.getRoutingKeyAuthorizationResult());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(properties.getExchange());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(properties.getMaxConcurrentConsumers());
        factory.setDefaultRequeueRejected(false); // Route unhandled failures to DLQ
        return factory;
    }
}
