package com.piyush.mockarena.controller;

import java.util.Map;
import java.util.HashMap;
import com.piyush.mockarena.dto.*;
import com.piyush.mockarena.service.SubmissionService;
import com.piyush.mockarena.service.CodeTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Code Submissions", description = "Code submission and execution management")
public class SubmissionController {

    private final SubmissionService submissionService;

    // ✅ NEW - Add CodeTemplateService
    @Autowired
    private CodeTemplateService codeTemplateService;

    @Operation(summary = "Submit code", description = "Submit code solution for a problem")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> submitCode(
            @Valid @RequestBody SubmissionRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            SubmissionResponse submission = submissionService.submitCode(request, username);
            log.info("Code submitted by user: {} for problem: {}", username, request.getProblemId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "submission", submission,
                    "message", "Code submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Error submitting code", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error submitting code: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @Operation(summary = "Get submission", description = "Retrieve submission by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSubmission(
            @Parameter(description = "Submission ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            SubmissionResponse submission = submissionService.getSubmissionById(id, authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "submission", submission
            ));
        } catch (Exception e) {
            log.error("Error fetching submission", e);
            return ResponseEntity.status(404).body(Map.of(
                    "message", "Submission not found: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @Operation(summary = "Get user submissions", description = "Get current user's submissions with pagination")
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getMySubmissions(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<SubmissionResponse> submissions = submissionService.getUserSubmissions(
                    authentication.getName(), pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", submissions.getContent());
            response.put("totalElements", submissions.getTotalElements());
            response.put("totalPages", submissions.getTotalPages());
            response.put("size", submissions.getSize());
            response.put("number", submissions.getNumber());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user submissions", e);
            return ResponseEntity.ok(Map.of(
                    "content", List.of(),
                    "totalElements", 0,
                    "totalPages", 0,
                    "size", size,
                    "number", page,
                    "message", "No submissions found",
                    "success", false
            ));
        }
    }

    @Operation(summary = "Get recent submissions", description = "Get recent submissions from all users")
    @GetMapping("/recent")
    public ResponseEntity<List<SubmissionResponse>> getRecentSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<SubmissionResponse> submissions = submissionService.getRecentSubmissions(pageable);
            return ResponseEntity.ok(submissions.getContent());
        } catch (Exception e) {
            log.error("Error fetching recent submissions", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "Get submission statistics", description = "Get user's submission statistics")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSubmissionStats(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSubmissions", 0);
        stats.put("acceptedSubmissions", 0);
        stats.put("acceptanceRate", 0.0);
        stats.put("username", username);
        stats.put("message", "Statistics will be available after making submissions");

        return ResponseEntity.ok(stats);
    }

    /**
     * ✅ SINGLE /run ENDPOINT - Run code directly without creating submission
     */
    @Operation(summary = "Run code", description = "Run code with custom input (for testing)")
    @PostMapping("/run")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> runCode(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            log.info("Code run request from user: {}", authentication.getName());

            Integer languageId = null;
            String sourceCode = null;
            String input = "";

            if (request.containsKey("languageId")) {
                languageId = (Integer) request.get("languageId");
            } else if (request.containsKey("language_id")) {
                languageId = (Integer) request.get("language_id");
            }

            if (request.containsKey("sourceCode")) {
                sourceCode = (String) request.get("sourceCode");
            } else if (request.containsKey("source_code")) {
                sourceCode = (String) request.get("source_code");
            }

            if (request.containsKey("input")) {
                input = (String) request.get("input");
            }

            if (languageId == null || sourceCode == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Missing required parameters: languageId and sourceCode",
                        "success", false,
                        "status", "ERROR"
                ));
            }

            if (input == null) input = "";

            log.info("Running code - Language: {}, Input length: {}, Code length: {}",
                    languageId, input.length(), sourceCode.length());

            Map<String, Object> result = submissionService.runCodeDirectly(languageId, sourceCode, input);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error running code", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error running code: " + e.getMessage(),
                    "success", false,
                    "stdout", null,
                    "stderr", null,
                    "compileOutput", null,
                    "runtimeMs", null,
                    "memoryKb", null
            ));
        }
    }

    // ✅ LEETCODE-STYLE TEMPLATE ENDPOINTS (CLEANED UP, NO DUPLICATES)

    @Operation(summary = "Get problem template", description = "Get template code for a specific problem and language")
    @GetMapping("/template/{problemId}/{languageId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getProblemTemplate(
            @Parameter(description = "Problem ID") @PathVariable Long problemId,
            @Parameter(description = "Language ID") @PathVariable Integer languageId,
            Authentication authentication) {
        try {
            log.info("Getting template for problem {} and language {} by user: {}",
                    problemId, languageId, authentication.getName());

            Map<String, Object> template = submissionService.getProblemTemplate(problemId, languageId);
            return ResponseEntity.ok(template);

        } catch (Exception e) {
            log.error("Error getting problem template", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "template", "// Error loading template\n// Please try again",
                    "language", "java",
                    "exampleTestCases", List.of()
            ));
        }
    }

    @Operation(summary = "Run code with template", description = "Run code against visible test cases (LeetCode-style)")
    @PostMapping("/run-template")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> runCodeWithTemplate(
            @RequestBody CodeRunWithTemplateRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Running code with template for user: {}", username);

            Map<String, Object> result = submissionService.runCodeWithTemplate(
                    request.getProblemId(),
                    request.getLanguageId(),
                    request.getUserCode(),
                    username
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error running code with template", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to run code: " + e.getMessage(),
                    "testResults", List.of(),
                    "passedTestCases", 0,
                    "totalTestCases", 0
            ));
        }
    }

    @Operation(summary = "Submit code with template", description = "Submit solution against all test cases")
    @PostMapping("/submit-template")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> submitCodeWithTemplate(
            @RequestBody SubmissionWithTemplateRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Submitting code with template for user: {}", username);

            SubmissionResponse result = submissionService.submitCodeWithTemplate(
                    request.getProblemId(),
                    request.getLanguageId(),
                    request.getUserCode(),
                    username
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "submission", result,
                    "message", "Solution submitted successfully!"
            ));

        } catch (Exception e) {
            log.error("Error submitting code with template", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to submit solution: " + e.getMessage()
            ));
        }
    }
}
