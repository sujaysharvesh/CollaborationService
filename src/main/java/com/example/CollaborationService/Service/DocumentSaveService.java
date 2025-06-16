package com.example.CollaborationService.Service;

import com.example.CollaborationService.DTO.DocumentSaveEvent;
import com.example.CollaborationService.Messager.RabbitConfig;
import com.example.CollaborationService.SessionServices.DocumentStateService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class DocumentSaveService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DocumentStateService documentStateService;

    public void saveDocument(String documentId, String userId) {

        byte[] currentState = documentStateService.getDocumentState(documentId);
        if(currentState != null) {
            DocumentSaveEvent saveEvent = DocumentSaveEvent.builder()
                    .documentId(documentId)
                    .userId(userId)
                    .content(currentState)
                    .timestamp(Instant.now())
                    .build();
            rabbitTemplate.convertAndSend("document.save.queue", saveEvent);
        }
    }

    @Scheduled(fixedRate = 60000) // 1 minute
    public void autoSaveDocuments() {
    }
}
