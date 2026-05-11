package com.kanade.backend.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_IMPORT = "queue.question.import";
    public static final String EXCHANGE_IMPORT = "exchange.question.import";
    public static final String ROUTING_KEY_IMPORT = "routing.key.question.import";

    // 批量批改队列
    public static final String QUEUE_CORRECTION = "queue.exam.correction";
    public static final String EXCHANGE_CORRECTION = "exchange.exam.correction";
    public static final String ROUTING_KEY_CORRECTION = "routing.key.exam.correction";

    @Bean
    public Queue importQueue() {
        return QueueBuilder.durable(QUEUE_IMPORT).build();
    }

    @Bean
    public DirectExchange importExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_IMPORT).durable(true).build();
    }

    @Bean
    public Binding importBinding(Queue importQueue, DirectExchange importExchange) {
        return BindingBuilder.bind(importQueue).to(importExchange).with(ROUTING_KEY_IMPORT);
    }

    // 批量批改队列配置
    @Bean
    public Queue correctionQueue() {
        return QueueBuilder.durable(QUEUE_CORRECTION).build();
    }

    @Bean
    public DirectExchange correctionExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_CORRECTION).durable(true).build();
    }

    @Bean
    public Binding correctionBinding(Queue correctionQueue, DirectExchange correctionExchange) {
        return BindingBuilder.bind(correctionQueue).to(correctionExchange).with(ROUTING_KEY_CORRECTION);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
