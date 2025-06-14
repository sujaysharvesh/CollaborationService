package com.example.CollaborationService.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
public class DocumentMetadata {
    private String documentId;
    private Instant lastModified;
    private String lastModifiedBy;
    private Set<String> activeUsers = new HashSet<>();

    public DocumentMetadata(String documentId){
        this.documentId = documentId;
        this.lastModified = Instant.now();
    }

    public static DocumentMetadata fromBytes(byte[] data) throws IOException {
        return new ObjectMapper().readValue(data, DocumentMetadata.class);
    }

    public byte[] toBytes() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsBytes(this);
    }

}
