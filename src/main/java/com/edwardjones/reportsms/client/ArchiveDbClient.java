package com.edwardjones.reportsms.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ArchiveDbClient {

    private static final Logger log = LoggerFactory.getLogger(ArchiveDbClient.class);

    private final WebClient archiveWebClient;

    @CircuitBreaker(name = "archiveDb", fallbackMethod = "fallbackArchive")
    @Retry(name = "archiveDb")
    public String archiveReport(String requestId, String payloadJson) {
        return archiveWebClient.post()
                .uri("/api/v1/archive/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payloadJson))
                .retrieve()
                .bodyToMono(ArchiveResponse.class)
                .map(ArchiveResponse::archiveRef)
                .doOnSuccess(ref -> log.info("Archived report {} with ref {}", requestId, ref))
                .block();
    }

    @SuppressWarnings("unused")
    private String fallbackArchive(String requestId, String payloadJson, Throwable ex) {
        log.error("Archive service unavailable for {}: {}", requestId, ex.getMessage());
        throw new ArchiveUnavailableException("Archive service unavailable", ex);
    }

    public record ArchiveResponse(String archiveRef) { }
}

