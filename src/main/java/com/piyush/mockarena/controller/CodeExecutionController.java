package com.piyush.mockarena.controller;

import com.piyush.mockarena.service.CodeExecutionService;
import com.piyush.mockarena.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/code")
@CrossOrigin(origins = "*")
@Slf4j
public class CodeExecutionController {

    @Autowired
    private CodeExecutionService codeExecutionService;

    /**
     * 🚀 RUN CODE: Quick test with sample test cases (LeetCode "Run Code")
     */
    @PostMapping("/run")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CodeExecutionResponse>> runCode(
            @Valid @RequestBody CodeRunRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("🔧 Running code for user: {} on problem: {}", username, request.getProblemId());

        // Mark as run (not submission)
        request.setIsSubmission(false);

        return codeExecutionService.executeCode(request, username)
                .thenApply(response -> ResponseEntity.ok(response))
                .exceptionally(throwable -> {
                    log.error("❌ Code run failed: {}", throwable.getMessage());
                    return ResponseEntity.badRequest()
                            .body(CodeExecutionResponse.builder()
                                    .success(false)
                                    .status("ERROR")
                                    .message(throwable.getMessage())
                                    .build());
                });
    }

    /**
     * 🎯 SUBMIT: Full submission with all test cases (LeetCode "Submit Solution")
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CodeExecutionResponse>> submitSolution(
            @Valid @RequestBody CodeRunRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("🎯 Submitting solution for user: {} on problem: {}", username, request.getProblemId());

        // Mark as submission
        request.setIsSubmission(true);

        return codeExecutionService.executeCode(request, username)
                .thenApply(response -> ResponseEntity.ok(response))
                .exceptionally(throwable -> {
                    log.error("❌ Solution submission failed: {}", throwable.getMessage());
                    return ResponseEntity.badRequest()
                            .body(CodeExecutionResponse.builder()
                                    .success(false)
                                    .status("ERROR")
                                    .message(throwable.getMessage())
                                    .build());
                });
    }

    /**
     * 📊 GET SUBMISSION STATUS: Check submission result
     */
    @GetMapping("/submission/{submissionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CodeExecutionResponse> getSubmissionStatus(
            @PathVariable Long submissionId,
            Authentication authentication) {

        try {
            CodeExecutionResponse result = codeExecutionService.getSubmissionDetails(submissionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CodeExecutionResponse.builder()
                            .success(false)
                            .status("ERROR")
                            .message(e.getMessage())
                            .build());
        }
    }
}
