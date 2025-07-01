package com.example.CollaborationService.Resilience;


import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.Retry;


@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate to open the circuit
                .waitDurationInOpenState(java.time.Duration.ofSeconds(30)) // Wait 30 seconds before trying again
                .slidingWindowSize(10) // Number of calls to consider for failure rate
                .minimumNumberOfCalls(5) // Minimum number of calls before circuit breaker is considered
                .build();
        return CircuitBreaker.of("DocumentService", config);
    }

    @Bean
    public Retry retry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3) // Maximum number of retry attempts
                .waitDuration(java.time.Duration.ofMillis(500)) // Wait 500ms between retries
                .retryExceptions(Exception.class) // Retry on any exception
                .build();
        return Retry.of("DocumentService", config);
    }

    @Bean
    public Bulkhead bulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(10) // Maximum number of concurrent calls
                .maxWaitDuration(java.time.Duration.ofMillis(1000)) // Maximum wait time for a call
                .build();
        return Bulkhead.of("DocumentService", config);
    }
}
