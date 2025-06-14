package com.example.CollaborationService.DTO;


import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DocumentSaveEvent {
    private String documentId;
    private String userId;
    private byte[] content;
    private Instant timestamp;
}
