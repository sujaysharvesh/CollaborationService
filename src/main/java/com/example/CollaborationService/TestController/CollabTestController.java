package com.example.CollaborationService.TestController;


import com.example.CollaborationService.Client.DocumentServiceClient;
import com.example.CollaborationService.DTO.DocumentResponseDTO;
import com.example.CollaborationService.DTO.DocumentSaveEvent;
import com.example.CollaborationService.DTO.TestDocument;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/collab/test")
public class CollabTestController {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DocumentServiceClient documentServiceClient;

    @GetMapping("/home")
    public String home() {
        return "Collaboration Service is running!";
    }

    @GetMapping("/{documentId}/{userId}")
    public Mono<ResponseEntity<DocumentResponseDTO>> documentContent(
            @PathVariable String documentId,
            @PathVariable String userId) {

        log.info("Collaboration service: Fetching document {} for user {}", documentId, userId);

        return documentServiceClient.getDocumentContent(documentId, userId)
                .map(responseDTO -> {
                    log.info("Successfully retrieved document content for documentId: {}", documentId);
                    return ResponseEntity.ok(responseDTO);
                })
                .onErrorResume(NotFoundException.class, ex -> {
                    log.error("Document not found: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                })
                .onErrorResume(ServiceUnavailableException.class, ex -> {
                    log.error("Service unavailable: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
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
