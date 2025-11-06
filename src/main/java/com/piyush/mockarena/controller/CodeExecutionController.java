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
        log.info("🔥 Code RUN request: Problem {} by {}", request.getProblemId(), username);

        return codeExecutionService.executeCode(request, username, false)
                .thenApply(response -> {
                    log.info("✅ Run completed for problem {} - Status: {}",
                            request.getProblemId(), response.getStatus());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Run failed for problem {}: {}",
                            request.getProblemId(), throwable.getMessage());
                    return ResponseEntity.status(500).body(
                            CodeExecutionResponse.builder()
                                    .success(false)
                                    .status("INTERNAL_ERROR")
                                    .message("Code execution failed: " + throwable.getMessage())
                                    .build()
                    );
                });
    }

    /**
     * 🎯 SUBMIT CODE: Full submission with all test cases (LeetCode "Submit")
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CodeExecutionResponse>> submitCode(
            @Valid @RequestBody CodeSubmitRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("🎯 Code SUBMIT request: Problem {} by {}", request.getProblemId(), username);

        // Convert to CodeRunRequest for unified processing
        CodeRunRequest runRequest = new CodeRunRequest();
        runRequest.setProblemId(request.getProblemId());
        runRequest.setLanguageId(request.getLanguageId());
        runRequest.setSourceCode(request.getCode());
        runRequest.setIsSubmission(true);

        return codeExecutionService.executeCode(runRequest, username, true)
                .thenApply(response -> {
                    log.info("✅ Submission completed for problem {} - Status: {}",
                            request.getProblemId(), response.getStatus());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Submission failed for problem {}: {}",
                            request.getProblemId(), throwable.getMessage());
                    return ResponseEntity.status(500).body(
                            CodeExecutionResponse.builder()
                                    .success(false)
                                    .status("INTERNAL_ERROR")
                                    .message("Submission failed: " + throwable.getMessage())
                                    .build()
                    );
                });
    }

    /**
     * 🔧 RUN WITH TEMPLATE: LeetCode-style function-only submission
     */
    @PostMapping("/run-template")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CodeExecutionResponse>> runWithTemplate(
            @Valid @RequestBody CodeRunWithTemplateRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("🔧 Template RUN request: Problem {} by {}", request.getProblemId(), username);

        return codeExecutionService.executeWithTemplate(request, username, false)
                .thenApply(response -> {
                    log.info("✅ Template run completed for problem {} - Status: {}",
                            request.getProblemId(), response.getStatus());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Template run failed for problem {}: {}",
                            request.getProblemId(), throwable.getMessage());
                    return ResponseEntity.status(500).body(
                            CodeExecutionResponse.builder()
                                    .success(false)
                                    .status("INTERNAL_ERROR")
                                    .message("Template execution failed: " + throwable.getMessage())
                                    .build()
                    );
                });
    }

    /**
     * 📝 SUBMIT WITH TEMPLATE: LeetCode-style function-only submission
     */
    @PostMapping("/submit-template")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CodeExecutionResponse>> submitWithTemplate(
            @Valid @RequestBody SubmissionWithTemplateRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("📝 Template SUBMIT request: Problem {} by {}", request.getProblemId(), username);

        // Convert to unified request format
        CodeRunWithTemplateRequest runRequest = CodeRunWithTemplateRequest.builder()
                .problemId(request.getProblemId())
                .languageId(request.getLanguageId())
                .userCode(request.getUserCode())
                .build();

        return codeExecutionService.executeWithTemplate(runRequest, username, true)
                .thenApply(response -> {
                    log.info("✅ Template submission completed for problem {} - Status: {}",
                            request.getProblemId(), response.getStatus());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Template submission failed for problem {}: {}",
                            request.getProblemId(), throwable.getMessage());
                    return ResponseEntity.status(500).body(
                            CodeExecutionResponse.builder()
                                    .success(false)
                                    .status("INTERNAL_ERROR")
                                    .message("Template submission failed: " + throwable.getMessage())
                                    .build()
                    );
                });
    }
}
