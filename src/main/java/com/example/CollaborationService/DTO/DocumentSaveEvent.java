package com.example.CollaborationService.DTO;


import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Getter
@Setter
public class DocumentSaveEvent {
    private String documentId;
    private String title;
    private String userId;
    private byte[] content;
    
    private Instant timestamp;
}
