package com.example.CollaborationService.TestController;


import com.example.CollaborationService.DTO.DocumentSaveEvent;
import com.example.CollaborationService.DTO.TestDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/collab/test")
public class CollabTestController {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/home")
    public String home() {
        return "Collaboration Service is running!";
    }

    @PostMapping("/updateDocument")
    public ResponseEntity<String> updateDocument(@RequestBody TestDocument testDocument) {
        DocumentSaveEvent event = DocumentSaveEvent.builder()
                .title(testDocument.getTitle())
                .userId(testDocument.getUserId())
                .documentId(testDocument.getDocumentId())
                .timestamp(Instant.now())
                .content(testDocument.getContent())
                .build();
        rabbitTemplate.convertAndSend("document.exchange","document.save", event);
        return ResponseEntity.ok("Document updated successfully: ");
    }

}
