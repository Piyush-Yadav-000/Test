package com.piyush.mockarena.service;

import java.util.Map;
import java.util.HashMap;
import reactor.core.publisher.Mono;
import com.piyush.mockarena.dto.*;
import com.piyush.mockarena.entity.*;
import com.piyush.mockarena.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
// ✅ REMOVED PROBLEMATIC IMPORTS
// import java.net.SocketException;  // REMOVED - causing compilation error
// import java.util.Arrays;  // REMOVED - replaced with ArrayList operations

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final LanguageRepository languageRepository;
    private final UserRepository userRepository;
    private final Judge0Service judge0Service;
    private final UserProfileService userProfileService;
    private final TestCaseRepository testCaseRepository;

    // ✅ CodeTemplateService injection
    @Autowired
    private CodeTemplateService codeTemplateService;

    // ✅ KEEP ALL YOUR EXISTING METHODS - Just enhanced runCodeDirectly

    public SubmissionResponse submitCode(SubmissionRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Language language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new RuntimeException("Language not found"));

        // Create submission entity
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(language);
        submission.setSourceCode(request.getSourceCode());
        submission.setStatus(Submission.Status.PENDING);
        submission.setCreatedAt(LocalDateTime.now());

        Submission saved = submissionRepository.save(submission);
        log.info("Code submitted by user: {} for problem: {}", username, problem.getTitle());

        // Submit to Judge0 asynchronously
        processSubmissionAsync(saved);
        return mapToSubmissionResponse(saved);
    }

    @Async
    public CompletableFuture<Void> processSubmissionAsync(Submission submission) {
        try {
            // Create Judge0 submission request with correct field names
            Judge0Service.Judge0SubmissionRequest judge0Request = new Judge0Service.Judge0SubmissionRequest();
            judge0Request.setSource_code(submission.getSourceCode()); // Note: snake_case
            judge0Request.setLanguage_id(submission.getLanguage().getId()); // Note: snake_case
            judge0Request.setStdin(""); // Default empty input for now
            judge0Request.setExpected_output(null); // Not needed for code execution

            // Submit to Judge0 using your existing service (reactive)
            judge0Service.submitCode(judge0Request)
                    .subscribe(
                            response -> {
                                submission.setJudge0Token(response.getToken());
                                submission.setStatus(Submission.Status.QUEUED);
                                submissionRepository.save(submission);

                                // Poll for results after a delay
                                pollSubmissionResultAsync(submission);
                            },
                            error -> {
                                log.error("Error submitting to Judge0: {}", error.getMessage());
                                submission.setStatus(Submission.Status.ERROR);
                                submission.setStderr("Error submitting to Judge0: " + error.getMessage());
                                submissionRepository.save(submission);
                            }
                    );

        } catch (Exception e) {
            log.error("Error processing submission: {}", submission.getId(), e);
            submission.setStatus(Submission.Status.ERROR);
            submission.setStderr("Internal server error");
            submissionRepository.save(submission);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public void pollSubmissionResultAsync(Submission submission) {
        try {
            // Wait before polling
            Thread.sleep(3000);

            // Poll for results
            for (int i = 0; i < 10; i++) {
                judge0Service.getSubmissionResult(submission.getJudge0Token())
                        .subscribe(
                                result -> {
                                    if (result.getStatus() != null && result.getStatus().getId() != null) {
                                        Integer statusId = result.getStatus().getId();
                                        // Check if processing is complete (not in queue or processing)
                                        if (statusId != 1 && statusId != 2) {
                                            updateSubmissionFromJudge0Result(submission, result);

                                            // Update user statistics
                                            userProfileService.updateSubmissionStats(submission.getUser().getUsername(), submission);

                                            // Update problem statistics
                                            updateProblemStats(submission);
                                        }
                                    }
                                },
                                error -> {
                                    log.error("Error polling Judge0 result: {}", error.getMessage());
                                    submission.setStatus(Submission.Status.ERROR);
                                    submission.setStderr("Error getting result from Judge0");
                                    submissionRepository.save(submission);
                                }
                        );
                Thread.sleep(2000); // Wait 2 seconds between polls
            }

        } catch (Exception e) {
            log.error("Error polling submission result: {}", submission.getId(), e);
            submission.setStatus(Submission.Status.ERROR);
            submissionRepository.save(submission);
        }
    }

    private void updateSubmissionFromJudge0Result(Submission submission, Judge0Service.Judge0ResultResponse result) {
        try {
            // Map Judge0 status to our submission status using your existing enum values
            if (result.getStatus() != null && result.getStatus().getId() != null) {
                Integer statusId = result.getStatus().getId();
                switch (statusId) {
                    case 3 -> submission.setStatus(Submission.Status.ACCEPTED);
                    case 4 -> submission.setStatus(Submission.Status.WRONG_ANSWER);
                    case 5 -> submission.setStatus(Submission.Status.TIME_LIMIT_EXCEEDED);
                    case 6 -> submission.setStatus(Submission.Status.ERROR); // Compilation Error
                    case 7 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (NZEC)
                    case 8 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (SIGFPE)
                    case 9 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (SIGKILL)
                    case 10 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (SIGSEGV)
                    case 11 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (SIGXFSZ)
                    case 12 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (SIGXCPU)
                    case 13 -> submission.setStatus(Submission.Status.ERROR); // Runtime Error (Other)
                    case 14 -> submission.setStatus(Submission.Status.ERROR); // Exec format error
                    default -> submission.setStatus(Submission.Status.ERROR);
                }
            }

            // Set output fields
            submission.setStdout(result.getStdout());
            submission.setStderr(result.getStderr());
            submission.setCompileOutput(result.getCompile_output());

            // Set performance metrics
            if (result.getTime() != null) {
                try {
                    submission.setRuntimeMs((int) (Double.parseDouble(result.getTime()) * 1000));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse runtime: {}", result.getTime());
                }
            }

            if (result.getMemory() != null) {
                submission.setMemoryKb(result.getMemory());
            }

            submission.setUpdatedAt(LocalDateTime.now());
            submissionRepository.save(submission);
            log.info("Submission {} updated with status: {}", submission.getId(), submission.getStatus());

        } catch (Exception e) {
            log.error("Error updating submission from Judge0 result: {}", e.getMessage());
            submission.setStatus(Submission.Status.ERROR);
            submissionRepository.save(submission);
        }
    }

    private void updateProblemStats(Submission submission) {
        Problem problem = submission.getProblem();
        problem.incrementSubmissions();

        if (submission.getStatus() == Submission.Status.ACCEPTED) {
            problem.incrementAcceptedSubmissions();
        }

        problemRepository.save(problem);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Long id, String username) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Check if user owns this submission or is admin
        if (!submission.getUser().getUsername().equals(username) &&
                !submission.getUser().getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Access denied");
        }

        return mapToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getUserSubmissions(String username, Pageable pageable) {
        Page<Submission> submissions = submissionRepository.findByUserUsernameOrderByCreatedAtDesc(username, pageable);
        List<SubmissionResponse> responses = submissions.stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, submissions.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getUserSubmissionsForProblem(String username, Long problemId) {
        List<Submission> submissions = submissionRepository
                .findByUserUsernameAndProblemIdOrderByCreatedAtDesc(username, problemId);

        return submissions.stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getRecentSubmissions(Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Submission> submissions = submissionRepository.findRecentSubmissions(since, pageable);
        List<SubmissionResponse> responses = submissions.stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, submissions.size());
    }

    // ✅ NEW - LeetCode-style run with template (ENHANCED ERROR HANDLING)
    public Map<String, Object> runCodeWithTemplate(Long problemId, Integer languageId, String userCode, String username) {
        try {
            log.info("🏃 Running LeetCode-style code for user: {} on problem: {}", username, problemId);

            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            // Get visible test cases only (first 3 for quick testing)
            List<TestCase> visibleTestCases = testCaseRepository.findVisibleTestCasesByProblemId(problemId);

            if (visibleTestCases.isEmpty()) {
                log.warn("No visible test cases found for problem: {}", problemId);
                return Map.of(
                        "status", "ERROR",
                        "message", "No test cases available for this problem",
                        "testResults", List.of(),
                        "passedTestCases", 0,
                        "totalTestCases", 0
                );
            }

            // Limit to first 3 test cases for quick run
            List<TestCase> testCasesToRun = visibleTestCases.subList(0, Math.min(3, visibleTestCases.size()));

            // Generate executable code with test wrapper
            String executableCode = codeTemplateService.generateExecutableCode(
                    userCode, problemId, languageId, testCasesToRun
            );

            log.info("🔧 Generated executable code for problem {}", problemId);

            // ✅ ENHANCED: Run with retry mechanism
            Map<String, Object> runResult = runCodeDirectlyWithRetry(languageId, executableCode, "");

            // ✅ ENHANCED: Parse test results from stdout
            List<Map<String, Object>> testResults = parseTestResults(
                    runResult.get("stdout") != null ? runResult.get("stdout").toString() : "",
                    testCasesToRun
            );

            // Count passed tests
            int passedCount = (int) testResults.stream().filter(tr -> (Boolean) tr.get("passed")).count();

            // ✅ FIXED: Return proper structure
            Map<String, Object> result = new HashMap<>();
            result.put("status", runResult.get("status"));
            result.put("testResults", testResults);
            result.put("totalTestCases", testCasesToRun.size());
            result.put("passedTestCases", passedCount);
            result.put("runtimeMs", runResult.get("runtimeMs"));
            result.put("memoryKb", runResult.get("memoryKb"));
            result.put("message", runResult.get("message"));
            result.put("stdout", runResult.get("stdout"));
            result.put("stderr", runResult.get("stderr"));
            result.put("compileOutput", runResult.get("compileOutput"));

            log.info("✅ LeetCode run completed: {}/{} tests passed", passedCount, testCasesToRun.size());
            return result;

        } catch (Exception e) {
            log.error("❌ Error running LeetCode-style code", e);
            return Map.of(
                    "status", "ERROR",
                    "message", "Failed to run code: " + e.getMessage(),
                    "testResults", List.of(),
                    "passedTestCases", 0,
                    "totalTestCases", 0
            );
        }
    }

    // ✅ FIXED: Universal test results parser that works with your schema
    private List<Map<String, Object>> parseTestResults(String stdout, List<TestCase> testCases) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (stdout == null || stdout.trim().isEmpty()) {
            // Create failed results for empty output
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                results.add(Map.of(
                        "caseNumber", i + 1,
                        "input", testCase.getInput(),                    // ✅ Using your schema
                        "expectedOutput", testCase.getExpectedOutput(), // ✅ Using your schema
                        "actualOutput", "No output",
                        "passed", false,
                        "visible", "PUBLIC".equals(testCase.getType().name()) // ✅ Using enum
                ));
            }
            return results;
        }

        log.info("📝 Parsing stdout: {}", stdout);

        // ✅ FIXED: UNIVERSAL PATTERNS - Work for any problem type (using ArrayList instead of Arrays.asList)
        List<Pattern> testPatterns = new ArrayList<>();
        testPatterns.add(Pattern.compile("Test\\s+(\\d+):\\s*(PASSED|FAILED|PASS|FAIL)", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("Test Case\\s+(\\d+):\\s*(PASSED|FAILED|PASS|FAIL)", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("Case\\s+(\\d+):\\s*(PASSED|FAILED|PASS|FAIL)", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("TC\\s+(\\d+):\\s*(PASSED|FAILED|PASS|FAIL)", Pattern.CASE_INSENSITIVE));

        // Summary patterns
        List<Pattern> summaryPatterns = new ArrayList<>();
        summaryPatterns.add(Pattern.compile("Results?:\\s*(\\d+)/(\\d+)\\s*test cases? passed", Pattern.CASE_INSENSITIVE));
        summaryPatterns.add(Pattern.compile("(\\d+)\\s*out of\\s*(\\d+)\\s*test cases? passed", Pattern.CASE_INSENSITIVE));
        summaryPatterns.add(Pattern.compile("(\\d+)/(\\d+)\\s*passed", Pattern.CASE_INSENSITIVE));
        summaryPatterns.add(Pattern.compile("Passed:\\s*(\\d+)\\s*/\\s*(\\d+)", Pattern.CASE_INSENSITIVE));

        String[] lines = stdout.split("\n");
        Map<Integer, Boolean> testStatuses = new HashMap<>();
        Map<Integer, String> actualOutputs = new HashMap<>();

        // ✅ Parse individual test results using multiple patterns
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Try each test pattern
            for (Pattern pattern : testPatterns) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int testNum = Integer.parseInt(matcher.group(1));
                    String status = matcher.group(2).toUpperCase();
                    boolean passed = status.equals("PASSED") || status.equals("PASS");

                    testStatuses.put(testNum, passed);

                    // Try to extract actual output from the line
                    extractActualOutput(line, testNum, actualOutputs);
                    break;
                }
            }
        }

        // ✅ Create results based on parsed individual test data
        if (!testStatuses.isEmpty()) {
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                int testNum = i + 1;
                boolean passed = testStatuses.getOrDefault(testNum, false);
                String actualOutput = actualOutputs.getOrDefault(testNum,
                        passed ? testCase.getExpectedOutput() : "Failed");

                results.add(Map.of(
                        "caseNumber", testNum,
                        "input", testCase.getInput(),                    // ✅ Using your schema
                        "expectedOutput", testCase.getExpectedOutput(), // ✅ Using your schema
                        "actualOutput", actualOutput,
                        "passed", passed,
                        "visible", "PUBLIC".equals(testCase.getType().name()) // ✅ Using enum
                ));
            }
        }
        // ✅ Fallback: Parse from summary if no individual results
        else {
            int passedCount = 0;

            for (String line : lines) {
                for (Pattern summaryPattern : summaryPatterns) {
                    Matcher summaryMatcher = summaryPattern.matcher(line);
                    if (summaryMatcher.find()) {
                        passedCount = Integer.parseInt(summaryMatcher.group(1));
                        break;
                    }
                }
                if (passedCount > 0) break;
            }

            // Create results based on summary count
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                boolean passed = i < passedCount;

                results.add(Map.of(
                        "caseNumber", i + 1,
                        "input", testCase.getInput(),                    // ✅ Using your schema
                        "expectedOutput", testCase.getExpectedOutput(), // ✅ Using your schema
                        "actualOutput", passed ? testCase.getExpectedOutput() : "Incorrect",
                        "passed", passed,
                        "visible", "PUBLIC".equals(testCase.getType().name()) // ✅ Using enum
                ));
            }
        }

        // ✅ Ultimate fallback: Success/failure analysis
        if (results.isEmpty()) {
            boolean overallSuccess = !stdout.toLowerCase().contains("fail") &&
                    !stdout.toLowerCase().contains("error") &&
                    !stdout.toLowerCase().contains("wrong") &&
                    !stdout.toLowerCase().contains("incorrect");

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);

                results.add(Map.of(
                        "caseNumber", i + 1,
                        "input", testCase.getInput(),                    // ✅ Using your schema
                        "expectedOutput", testCase.getExpectedOutput(), // ✅ Using your schema
                        "actualOutput", overallSuccess ? testCase.getExpectedOutput() : "Unknown",
                        "passed", overallSuccess,
                        "visible", "PUBLIC".equals(testCase.getType().name()) // ✅ Using enum
                ));
            }
        }

        log.info("🔍 Parsed {} test results from stdout", results.size());
        return results;
    }

    // ✅ FIXED: Helper method to extract actual output values
    private void extractActualOutput(String line, int testNum, Map<Integer, String> actualOutputs) {
        // Look for common output patterns in the line
        List<Pattern> outputPatterns = new ArrayList<>();
        outputPatterns.add(Pattern.compile("Expected:\\s*([^,]+),?\\s*(?:Got|Actual):\\s*([^,\\s]+)", Pattern.CASE_INSENSITIVE));
        outputPatterns.add(Pattern.compile("Output:\\s*([^,\\s]+)", Pattern.CASE_INSENSITIVE));
        outputPatterns.add(Pattern.compile("->\\s*([^,\\s]+)", Pattern.CASE_INSENSITIVE));
        outputPatterns.add(Pattern.compile("Result:\\s*([^,\\s]+)", Pattern.CASE_INSENSITIVE));

        for (Pattern pattern : outputPatterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String output = matcher.groupCount() >= 2 ? matcher.group(2) : matcher.group(1);
                actualOutputs.put(testNum, output.trim());
                break;
            }
        }
    }

    // ✅ NEW - LeetCode-style submit with template
    public SubmissionResponse submitCodeWithTemplate(Long problemId, Integer languageId, String userCode, String username) {
        try {
            log.info("🚀 Submitting LeetCode-style code for user: {} on problem: {}", username, problemId);

            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not found"));

            // Get ALL test cases (both visible and hidden)
            List<TestCase> allTestCases = testCaseRepository.findAllTestCasesByProblemId(problemId);

            if (allTestCases.isEmpty()) {
                throw new RuntimeException("No test cases found for problem: " + problemId);
            }

            // Generate executable code with ALL test cases
            String executableCode = codeTemplateService.generateExecutableCode(
                    userCode, problemId, languageId, allTestCases
            );

            // Create submission entity with ORIGINAL user code (not wrapped)
            Submission submission = new Submission();
            submission.setUser(user);
            submission.setProblem(problem);
            submission.setLanguage(language);
            submission.setSourceCode(userCode); // Store original user code
            submission.setStatus(Submission.Status.PENDING);
            submission.setCreatedAt(LocalDateTime.now());

            Submission saved = submissionRepository.save(submission);

            // Submit the EXECUTABLE code to Judge0 (with wrapper)
            processSubmissionAsyncWithTemplate(saved, executableCode);

            return mapToSubmissionResponse(saved);

        } catch (Exception e) {
            log.error("❌ Error submitting LeetCode-style code: {}", e.getMessage());
            throw new RuntimeException("Failed to submit solution: " + e.getMessage());
        }
    }

    // ✅ NEW - Process submission with template (async)
    @Async
    public CompletableFuture<Void> processSubmissionAsyncWithTemplate(Submission submission, String executableCode) {
        try {
            // Create Judge0 request with executable code (wrapped)
            Judge0Service.Judge0SubmissionRequest judge0Request = new Judge0Service.Judge0SubmissionRequest();
            judge0Request.setSource_code(executableCode); // Use wrapped executable code
            judge0Request.setLanguage_id(submission.getLanguage().getId());
            judge0Request.setStdin("");
            judge0Request.setExpected_output(null);

            // Submit to Judge0
            judge0Service.submitCode(judge0Request)
                    .subscribe(
                            response -> {
                                submission.setJudge0Token(response.getToken());
                                submission.setStatus(Submission.Status.QUEUED);
                                submissionRepository.save(submission);

                                // Poll for results
                                pollSubmissionResultAsync(submission);
                            },
                            error -> {
                                log.error("❌ Error submitting to Judge0: {}", error.getMessage());
                                submission.setStatus(Submission.Status.ERROR);
                                submission.setStderr("Error submitting to Judge0: " + error.getMessage());
                                submissionRepository.save(submission);
                            }
                    );

        } catch (Exception e) {
            log.error("❌ Error processing template submission: {}", e.getMessage());
            submission.setStatus(Submission.Status.ERROR);
            submission.setStderr("Internal server error");
            submissionRepository.save(submission);
        }

        return CompletableFuture.completedFuture(null);
    }

    // ✅ ENHANCED: Run code directly with retry mechanism for connection issues
    public Map<String, Object> runCodeDirectlyWithRetry(Integer languageId, String sourceCode, String input) {
        int maxRetries = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🔄 Code execution attempt {} of {}", attempt, maxRetries);

                Map<String, Object> result = runCodeDirectly(languageId, sourceCode, input);

                // If successful, return immediately
                if (!"ERROR".equals(result.get("status"))) {
                    return result;
                }

                // If it's a connection error, retry
                String message = (String) result.get("message");
                if (message != null && (message.contains("Connection reset") ||
                        message.contains("timeout") ||
                        message.contains("temporarily unavailable"))) {
                    lastException = new RuntimeException(message);
                    if (attempt < maxRetries) {
                        log.warn("⚠️ Connection issue detected, retrying in {}s...", attempt);
                        Thread.sleep(attempt * 1000); // Progressive backoff
                        continue;
                    }
                }

                return result; // Return non-connection error immediately

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries && isRetryableException(e)) {
                    log.warn("⚠️ Retryable error on attempt {}: {}", attempt, e.getMessage());
                    try {
                        Thread.sleep(attempt * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        // All retries failed
        return Map.of(
                "status", "ERROR",
                "message", "Service temporarily unavailable after " + maxRetries + " attempts. Please try again later.",
                "lastError", lastException != null ? lastException.getMessage() : "Unknown error",
                "stdout", null,
                "stderr", null,
                "compileOutput", null,
                "runtimeMs", null,
                "memoryKb", null
        );
    }

    // ✅ FIXED: Check if exception is retryable (removed SocketException reference)
    private boolean isRetryableException(Exception e) {
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();

        return "SocketException".equals(className) ||
                e instanceof java.net.ConnectException ||
                e instanceof java.util.concurrent.TimeoutException ||
                (message != null && (message.contains("Connection reset") ||
                        message.contains("timeout") ||
                        message.contains("temporarily unavailable")));
    }

    /**
     * ✅ COMPLETELY FIXED: Run code directly - Generic error handling without SocketException
     */
    public Map<String, Object> runCodeDirectly(Integer languageId, String sourceCode, String input) {
        log.info("Running code directly - Language ID: {}", languageId);
        try {
            // Validate language exists
            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not supported: " + languageId));

            log.info("Language found: {}", language.getDisplayName());

            // Prepare Judge0 request
            Judge0Service.Judge0SubmissionRequest judge0Request = Judge0Service.Judge0SubmissionRequest.builder()
                    .source_code(sourceCode)
                    .language_id(languageId)
                    .stdin(input)
                    .build();

            // Submit to Judge0 and wait for result
            Mono<Judge0Service.Judge0SubmissionResponse> submissionMono = judge0Service.submitCode(judge0Request);
            Judge0Service.Judge0SubmissionResponse judge0Response = submissionMono.block();

            if (judge0Response == null || judge0Response.getToken() == null) {
                log.error("Failed to get token from Judge0");
                return Map.of(
                        "status", "ERROR",
                        "message", "Judge0 service temporarily unavailable. Please try again.",
                        "stdout", null,
                        "stderr", null,
                        "compileOutput", null,
                        "runtimeMs", null,
                        "memoryKb", null
                );
            }

            String token = judge0Response.getToken();
            log.info("Got Judge0 token: {}", token);

            // Poll for result
            Judge0Service.Judge0ResultResponse result = pollForResultSync(token, 15);

            if (result == null) {
                return Map.of(
                        "status", "TIMEOUT",
                        "message", "Code execution timed out",
                        "stdout", null,
                        "stderr", null,
                        "compileOutput", null,
                        "runtimeMs", null,
                        "memoryKb", null
                );
            }

            // Parse results
            String status = "COMPLETED";
            Integer runtimeMs = null;
            Integer memoryKb = result.getMemory();

            if (result.getTime() != null) {
                try {
                    double timeSeconds = Double.parseDouble(result.getTime());
                    runtimeMs = (int) (timeSeconds * 1000);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse execution time: {}", result.getTime());
                }
            }

            // Determine final status
            if (result.getStatus() != null) {
                Integer statusId = result.getStatus().getId();
                switch (statusId) {
                    case 3 -> status = "SUCCESS";
                    case 4 -> status = "WRONG_ANSWER";
                    case 5 -> status = "TIME_LIMIT_EXCEEDED";
                    case 6 -> status = "COMPILATION_ERROR";
                    case 7 -> status = "RUNTIME_ERROR";
                    default -> status = "ERROR";
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("stdout", result.getStdout());
            response.put("stderr", result.getStderr());
            response.put("compileOutput", result.getCompile_output());
            response.put("runtimeMs", runtimeMs);
            response.put("memoryKb", memoryKb);
            response.put("message", result.getMessage());
            response.put("language", language.getDisplayName());
            response.put("executionTime", result.getTime());

            log.info("Code execution completed - Status: {}, Runtime: {}ms", status, runtimeMs);
            return response;

        } catch (Exception e) {
            log.error("❌ Error during direct code execution", e);

            // Check if it's a connection-related error by examining class name and message
            String errorMessage = e.getMessage();
            String className = e.getClass().getSimpleName();

            if ("SocketException".equals(className) ||
                    (errorMessage != null && (errorMessage.contains("Connection reset") ||
                            errorMessage.contains("timeout") ||
                            errorMessage.contains("temporarily unavailable")))) {
                return Map.of(
                        "status", "ERROR",
                        "message", "Judge0 service temporarily unavailable. Please try again.",
                        "stdout", null,
                        "stderr", null,
                        "compileOutput", null,
                        "runtimeMs", null,
                        "memoryKb", null
                );
            }

            return Map.of(
                    "status", "ERROR",
                    "message", "Execution error: " + e.getMessage(),
                    "stdout", null,
                    "stderr", null,
                    "compileOutput", null,
                    "runtimeMs", null,
                    "memoryKb", null
            );
        }
    }

    /**
     * Synchronous polling for Judge0 results
     */
    private Judge0Service.Judge0ResultResponse pollForResultSync(String token, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(2000); // Wait 2 seconds between polls

                Mono<Judge0Service.Judge0ResultResponse> resultMono = judge0Service.getSubmissionResult(token);
                Judge0Service.Judge0ResultResponse result = resultMono.block();

                if (result != null && result.getStatus() != null) {
                    Integer statusId = result.getStatus().getId();
                    log.info("Judge0 polling attempt {}: Status ID = {}", i + 1, statusId);

                    // Status IDs: 1=In Queue, 2=Processing, 3=Accepted, 4=Wrong Answer, etc.
                    if (statusId != 1 && statusId != 2) { // Not in queue or processing
                        return result;
                    }
                }

            } catch (Exception e) {
                log.error("Error during polling attempt {}: {}", i + 1, e.getMessage());
                break;
            }
        }
        return null; // Timeout or error
    }

    // ✅ NEW - Get problem template
    public Map<String, Object> getProblemTemplate(Long problemId, Integer languageId) {
        try {
            String template = codeTemplateService.getTemplate(problemId, languageId);

            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not found"));

            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            // Get visible test cases for examples
            List<TestCase> visibleTestCases = testCaseRepository.findVisibleTestCasesByProblemId(problemId);

            return Map.of(
                    "template", template,
                    "language", language.getDisplayName(),
                    "imports", codeTemplateService.getImports(language.getDisplayName().toLowerCase()),
                    "problemTitle", problem.getTitle(),
                    "functionName", problem.getFunctionName() != null ? problem.getFunctionName() : "solution",
                    "returnType", problem.getReturnType() != null ? problem.getReturnType() : "int",
                    "usesTemplate", problem.isTemplateEnabled(),
                    "exampleTestCases", visibleTestCases.stream()
                            .limit(3)
                            .map(tc -> Map.of("input", tc.getInput(), "output", tc.getExpectedOutput()))
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            log.error("Error getting problem template: {}", e.getMessage());
            return Map.of(
                    "template", codeTemplateService.getDefaultTemplate(languageId),
                    "language", "java",
                    "imports", "",
                    "problemTitle", "Problem " + problemId,
                    "functionName", "solution",
                    "returnType", "int",
                    "usesTemplate", true,
                    "exampleTestCases", List.of(),
                    "error", e.getMessage()
            );
        }
    }

    // ✅ Keep all your existing helper methods...
    private SubmissionResponse mapToSubmissionResponse(Submission submission) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setProblemId(submission.getProblem().getId());
        response.setProblemTitle(submission.getProblem().getTitle());
        response.setLanguage(submission.getLanguage().getDisplayName());
        response.setStatus(submission.getStatus().name());
        response.setSourceCode(submission.getSourceCode());
        response.setRuntimeMs(submission.getRuntimeMs());
        response.setMemoryKb(submission.getMemoryKb());
        response.setStdout(submission.getStdout());
        response.setStderr(submission.getStderr());
        response.setCompileOutput(submission.getCompileOutput());
        response.setCreatedAt(submission.getCreatedAt());
        response.setUpdatedAt(submission.getUpdatedAt());
        return response;
    }
}
