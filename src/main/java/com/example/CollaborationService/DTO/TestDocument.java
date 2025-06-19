package com.example.CollaborationService.DTO;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.processing.Generated;
import java.util.UUID;


@Getter
@Setter
public class TestDocument {
    private String userId;
    private String title;
    private String documentId;
    private byte[] content;
}
