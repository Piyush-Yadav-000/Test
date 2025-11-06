package com.piyush.mockarena.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.SocketException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class Judge0Service {

    private final WebClient webClient;

    @Value("${mockarena.judge0.base-url:https://ce.judge0.com}")
    private String judge0BaseUrl;

    @Value("${mockarena.judge0.api-key:}")
    private String apiKey;

    @Value("${mockarena.judge0.max-timeout:30000}")
    private int maxTimeout;

    /**
     * Submit code to Judge0 for execution with enhanced error handling
     */
    public Mono<Judge0SubmissionResponse> submitCode(Judge0SubmissionRequest request) {
        log.info("🚀 Submitting code to Judge0 for language ID: {}", request.getLanguage_id());

        WebClient.RequestHeadersSpec<?> requestSpec = webClient
                .post()
                .uri(judge0BaseUrl + "/submissions?base64_encoded=false&wait=false")
                .bodyValue(request);

        // Add API key if available
        if (apiKey != null && !apiKey.isEmpty()) {
            requestSpec = requestSpec.header("X-RapidAPI-Key", apiKey);
        }

        return requestSpec
                .retrieve()
                .bodyToMono(Judge0SubmissionResponse.class)
                .timeout(Duration.ofMillis(maxTimeout))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                log.warn("🔄 Retrying Judge0 submission (attempt {}): {}",
                                        retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnNext(response -> log.info("✅ Code submitted to Judge0, token: {}", response.getToken()))
                .doOnError(error -> log.error("❌ Failed to submit to Judge0: {}", error.getMessage()))
                .onErrorMap(this::mapToJudge0Exception);
    }

    /**
     * Get submission result from Judge0 with enhanced error handling
     */
    public Mono<Judge0ResultResponse> getSubmissionResult(String token) {
        log.info("🔍 Fetching result from Judge0 for token: {}", token);

        if (token == null || token.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Token cannot be null or empty"));
        }

        WebClient.RequestHeadersSpec<?> requestSpec = webClient
                .get()
                .uri(judge0BaseUrl + "/submissions/" + token + "?base64_encoded=false");

        // Add API key if available
        if (apiKey != null && !apiKey.isEmpty()) {
            requestSpec = requestSpec.header("X-RapidAPI-Key", apiKey);
        }

        return requestSpec
                .retrieve()
                .bodyToMono(Judge0ResultResponse.class)
                .timeout(Duration.ofMillis(maxTimeout))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                log.warn("🔄 Retrying Judge0 result fetch (attempt {}): {}",
                                        retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnNext(result -> log.info("✅ Got result from Judge0: status={}, time={}",
                        result.getStatus() != null ? result.getStatus().getId() : "null",
                        result.getTime()))
                .doOnError(error -> log.error("❌ Failed to get result from Judge0: {}", error.getMessage()))
                .onErrorMap(this::mapToJudge0Exception);
    }

    /**
     * Check if error is retryable
     */
    private boolean isRetryableError(Throwable throwable) {
        // Retry on network/connection issues
        return throwable instanceof SocketException ||
                throwable instanceof java.net.ConnectException ||
                throwable instanceof java.util.concurrent.TimeoutException ||
                throwable instanceof WebClientException ||
                throwable.getMessage().contains("Connection reset") ||
                throwable.getMessage().contains("Connection refused") ||
                throwable.getMessage().contains("timeout");
    }

    /**
     * Map generic exceptions to more specific Judge0 exceptions
     */
    private Throwable mapToJudge0Exception(Throwable throwable) {
        if (throwable instanceof SocketException &&
                throwable.getMessage().contains("Connection reset")) {
            return new RuntimeException("Judge0 service temporarily unavailable. Please try again.", throwable);
        }

        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return new RuntimeException("Judge0 service timeout. The service may be overloaded.", throwable);
        }

        return throwable; // Return original exception if not mapped
    }

    /**
     * Get supported languages from Judge0
     */
    public Mono<Judge0Language[]> getSupportedLanguages() {
        log.info("🔍 Fetching supported languages from Judge0");

        WebClient.RequestHeadersSpec<?> requestSpec = webClient
                .get()
                .uri(judge0BaseUrl + "/languages");

        // Add API key if available
        if (apiKey != null && !apiKey.isEmpty()) {
            requestSpec = requestSpec.header("X-RapidAPI-Key", apiKey);
        }

        return requestSpec
                .retrieve()
                .bodyToMono(Judge0Language[].class)
                .timeout(Duration.ofMillis(maxTimeout))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(this::isRetryableError))
                .doOnNext(languages -> log.info("✅ Retrieved {} languages from Judge0", languages.length))
                .doOnError(error -> log.error("❌ Failed to get languages from Judge0: {}", error.getMessage()));
    }





    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Judge0ResultResponse {
        private String stdout;
        private String time;
        private Integer memory;
        private String stderr;
        private String token;
        private String compile_output;
        private String message;
        private Judge0Status status;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Judge0Language {
        private Integer id;
        private String name;
    }


    // Add these DTOs to Judge0Service.java

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Judge0SubmissionRequest {
        private String source_code;
        private Integer language_id;
        private String stdin;
        private String expected_output;
        private Float cpu_time_limit;
        private Integer memory_limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Judge0SubmissionResponse {
        private String stdout;
        private String stderr;
        private String compile_output;
        private String message;
        private Float time;
        private Integer memory;
        private Judge0Status status;
        private String token;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Judge0Status {
        private Integer id;
        private String description;
    }

}
