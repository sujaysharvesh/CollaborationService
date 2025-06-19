package com.example.CollaborationService.RabbitMQ;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Phased;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Bean(name = "saveQueue")
    public Queue documentSaveQueue() {
        return QueueBuilder.durable("document.save.queue").build();
    }

    @Bean
    public Queue documentSaveCompleteQueue() {
        return QueueBuilder.durable("document.save.complete.queue").build();
    }

    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange("document.exchange");
    }

    @Bean
    public Binding saveQueueBinding(@Qualifier("saveQueue") Queue documentSaveQueue,
                                    TopicExchange documentExchange) {
        return BindingBuilder
                .bind(documentSaveQueue)
                .to(documentExchange)
                .with("document.save");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

}
