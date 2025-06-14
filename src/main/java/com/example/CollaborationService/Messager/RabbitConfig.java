package com.example.CollaborationService.Messager;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Bean
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
    public Binding documentSaveBinding() {
        return BindingBuilder
                .bind(documentSaveQueue())
                .to(documentExchange())
                .with("document.save");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;

    }

}
