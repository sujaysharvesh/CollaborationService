package com.example.CollaborationService.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
public class DocumentMetadata {
    private String documentId;

    @JsonSerialize(using = InstantSerializer.class)
    private Instant lastModified;

    private String lastModifiedBy;
    private Set<String> activeUsers = new HashSet<>();

    public DocumentMetadata(String documentId){
        this.documentId = documentId;
        this.lastModified = Instant.now();
    }

    public static DocumentMetadata fromBytes(byte[] data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(data, DocumentMetadata.class);
    }

    public byte[] toBytes() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.writeValueAsBytes(this);
    }

}
