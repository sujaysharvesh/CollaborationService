package com.example.CollaborationService.Client;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.document-client")
public class DocumentClientProperties {
    private String baseUrl = "http://localhost:4005";
    private String getDocumentByIdEndpoint = "/api/v1/document/{documentId}/{userId}";
    private int timeout = 50000;
    private int maxRetries = 3;
    private int readTimeout = 5000;
    private int retryDelay = 1000;
}
