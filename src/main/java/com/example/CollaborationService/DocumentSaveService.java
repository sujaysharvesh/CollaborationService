package com.example.CollaborationService;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class DocumentSaveService {

    @Autowired
    private RabbitTemplate rabbitTemplate;


}
