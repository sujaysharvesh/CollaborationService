package com.example.CollaborationService.Client;


import com.example.CollaborationService.ApiResponse;
import com.example.CollaborationService.DTO.DocumentResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.print.Doc;
import java.net.http.HttpClient;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class DocumentServiceClient {


    private final DocumentClientProperties properties;
    private final WebClient documentWebClient;

    @Autowired
    public DocumentServiceClient(DocumentClientProperties properties,
                                 @Qualifier("documentWebClient") WebClient documentWebClient) {
        this.properties = properties;
        this.documentWebClient = documentWebClient;
        log.info("DocumentServiceClient initialized with base URL: {}", properties.getBaseUrl());
    }

    @CircuitBreaker(name = "DocumentService", fallbackMethod = "fallbackGetDocumentContent")
    public Mono<DocumentResponseDTO> getDocumentContent(String documentId, String userId) {
        log.info("Fetching document content for documentId: {} and userId: {}", documentId, userId);

        return documentWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getGetDocumentByIdEndpoint())
                        .build(documentId, userId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new NotFoundException("Document not found"));
                    } else if (response.statusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.error(new RuntimeException("Access denied"));
                    }
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<DocumentResponseDTO>>() {})
                .doOnNext(response -> {
                    log.info("Received ApiResponse: success={}, message={}, hasData={}",
                            response.isSuccess(), response.getMessage(), response.getData() != null);
                })
                .flatMap(apiResponse -> {
                    if (!apiResponse.isSuccess()) {
                        log.error("API Response indicates failure: {}", apiResponse.getError());
                        return Mono.error(new RuntimeException("API Error: " + apiResponse.getError()));
                    }

                    if (apiResponse.getData() == null) {
                        log.error("API Response data is null for documentId: {} and userId: {}", documentId, userId);
                        return Mono.error(new NotFoundException("Document not found or data is null"));
                    }

                    return Mono.just(apiResponse.getData());
                })
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofMillis(properties.getRetryDelay()))
                        .filter(throwable -> !(throwable instanceof NotFoundException) &&
                                !(throwable instanceof RuntimeException)))
                .doOnError(error -> log.error("Error fetching document content: {}", error.getMessage()))
                .onErrorMap(TimeoutException.class, ex ->
                        new ServiceUnavailableException("Document service timeout after " + properties.getTimeout() + "ms"));
    }

        public Mono<DocumentResponseDTO> fallbackGetDocumentContent(String documentId, String userId, Exception ex) {
        return Mono.error(
                new ServiceUnavailableException("Document service is currently unavailable. Please try again later"));
    }

}
