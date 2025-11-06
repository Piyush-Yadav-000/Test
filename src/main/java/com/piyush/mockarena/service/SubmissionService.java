package com.piyush.mockarena.service;
import java.util.stream.Collectors;

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

    // ✅ KEEP ALL YOUR EXISTING METHODS EXACTLY AS THEY ARE

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

    /**
     * ✅ FIXED: Run code with template method - NOW TESTS ALL CASES
     * This method runs user code against ALL test cases to provide comprehensive feedback
     */
    public Map<String, Object> runCodeWithTemplate(Long problemId, Integer languageId, String userCode, String username) {
        try {
            log.info("🏃 Running LeetCode-style code for user: {} on problem: {}", username, problemId);

            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            // 🚀 CRITICAL FIX: Get ALL test cases for comprehensive testing (not just visible ones)
            List<TestCase> allTestCases = testCaseRepository.findByProblemIdOrderBySortOrderAsc(problemId);

            if (allTestCases.isEmpty()) {
                log.warn("❌ No test cases found for problem: {}", problemId);
                // ✅ FIXED - Use HashMap instead of Map.of()
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "ERROR");
                errorResult.put("message", "No test cases available for this problem");
                errorResult.put("testResults", List.of());
                errorResult.put("passedTestCases", 0);
                errorResult.put("totalTestCases", 0);
                return errorResult;
            }

            // 🎯 NEW: Run ALL test cases (not just first 3) for comprehensive testing
            List<TestCase> testCasesToRun = allTestCases;
            log.info("🧪 Running ALL {} test cases for comprehensive testing", testCasesToRun.size());

            // Generate executable code with test wrapper using ALL test cases
            String executableCode = codeTemplateService.generateExecutableCode(
                    userCode, problemId, languageId, testCasesToRun
            );

            log.info("🔧 Generated executable code for problem {} with {} test cases", problemId, testCasesToRun.size());

            // Run with retry mechanism
            Map<String, Object> runResult = runCodeDirectlyWithRetry(languageId, executableCode, "");

            // Parse test results from stdout using ALL test cases
            List<Map<String, Object>> testResults = parseTestResults(
                    runResult.get("stdout") != null ? runResult.get("stdout").toString() : "",
                    testCasesToRun
            );

            // Count passed tests
            int passedCount = (int) testResults.stream().filter(tr -> (Boolean) tr.get("passed")).count();

            // 🚀 ENHANCED: Log detailed results for debugging
            log.info("📊 Test Results Summary:");
            log.info("   - Total test cases: {}", testCasesToRun.size());
            log.info("   - Passed: {}", passedCount);
            log.info("   - Failed: {}", testCasesToRun.size() - passedCount);

            // Log failed test cases for debugging
            for (int i = 0; i < testResults.size(); i++) {
                Map<String, Object> result = testResults.get(i);
                boolean passed = (Boolean) result.get("passed");
                if (!passed) {
                    log.info("❌ Test Case {}: FAILED - Expected: '{}', Got: '{}'",
                            i + 1, result.get("expectedOutput"), result.get("actualOutput"));
                }
            }

            // Return proper structure
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
            // ✅ FIXED - Use HashMap instead of Map.of()
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("message", "Failed to run code: " + e.getMessage());
            errorResult.put("testResults", List.of());
            errorResult.put("passedTestCases", 0);
            errorResult.put("totalTestCases", 0);
            return errorResult;
        }
    }

    // ✅ KEEP ALL YOUR EXISTING PARSING METHODS (they're perfect)
    /**
     * ✅ CRITICAL FIX: Parse test results with CORRECT logic AND field names
     */
    /**
     * ✅ ENHANCED: Parse test results with ACTUAL INPUT DISPLAY - FIXED VERSION
     */
    private List<Map<String, Object>> parseTestResults(String stdout, List<TestCase> testCases) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (stdout == null || stdout.isEmpty()) {
            log.warn("⚠️ No stdout output to parse");
            // Create default failed results for all test cases WITH ACTUAL INPUTS
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                Map<String, Object> result = new HashMap<>();
                result.put("input", formatInputForDisplay(testCase.getInput())); // ✅ FIXED: Show actual input
                result.put("expectedOutput", testCase.getExpectedOutput());
                result.put("actualOutput", "No output");
                result.put("passed", false);
                result.put("visible", testCase.getType() == TestCase.Type.PUBLIC);
                results.add(result);
            }
            return results;
        }

        log.debug("🔍 Parsing stdout output: {}", stdout);

        String[] lines = stdout.split("\n");
        Map<Integer, Map<String, Object>> testDataMap = new HashMap<>();

        // Initialize test data for all test cases
        for (int i = 1; i <= testCases.size(); i++) {
            testDataMap.put(i, new HashMap<>());
        }

        // Parse stdout lines to extract test results
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // ✅ ENHANCED: Extract actual input values from DEBUG lines
            if (line.matches("\\[DEBUG\\] Test Case \\d+ - Input: '.*'")) {
                Pattern pattern = Pattern.compile("\\[DEBUG\\] Test Case (\\d+) - Input: '(.*)'");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int testNum = Integer.parseInt(matcher.group(1));
                    String actualInput = matcher.group(2);
                    testDataMap.get(testNum).put("actualInput", actualInput);
                    log.debug("🎯 Extracted actual input for Test {}: '{}'", testNum, actualInput);
                }
            }

            // Look for [DEBUG] Test Case X - Result: True/False patterns FIRST
            else if (line.matches("\\[DEBUG\\] Test Case \\d+ - Result: (True|False)")) {
                Pattern pattern = Pattern.compile("\\[DEBUG\\] Test Case (\\d+) - Result: (True|False)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int testNum = Integer.parseInt(matcher.group(1));
                    boolean testPassed = "True".equals(matcher.group(2));
                    testDataMap.get(testNum).put("debugResult", testPassed);
                    log.debug("🔍 Test {}: DEBUG Result = {}", testNum, testPassed);
                }
            }

            // Look for Test Case X: PASS/FAIL patterns
            else if (line.matches("Test Case \\d+: (PASS|FAIL)")) {
                Pattern pattern = Pattern.compile("Test Case (\\d+): (PASS|FAIL)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int testNum = Integer.parseInt(matcher.group(1));
                    String status = matcher.group(2);
                    boolean testPassed = "PASS".equals(status);
                    testDataMap.get(testNum).put("status", testPassed);
                    log.debug("🔍 Test {}: Status = {}", testNum, status);
                }
            }

            // Look for expected/actual patterns
            else if (line.matches("\\[DEBUG\\] Test Case \\d+ - Expected: '.*'")) {
                Pattern pattern = Pattern.compile("\\[DEBUG\\] Test Case (\\d+) - Expected: '(.*)'");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int testNum = Integer.parseInt(matcher.group(1));
                    String expected = matcher.group(2);
                    testDataMap.get(testNum).put("expected", expected);
                }
            }

            else if (line.matches("\\[DEBUG\\] Test Case \\d+ - Actual: '.*'")) {
                Pattern pattern = Pattern.compile("\\[DEBUG\\] Test Case (\\d+) - Actual: '(.*)' ");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int testNum = Integer.parseInt(matcher.group(1));
                    String actual = matcher.group(2);
                    testDataMap.get(testNum).put("actual", actual);
                }
            }
        }

        // Extract summary line
        int passedTests = 0;
        int totalTests = testCases.size();
        Pattern summaryPattern = Pattern.compile("Results: (\\d+)/(\\d+) test cases passed");
        for (String line : lines) {
            Matcher matcher = summaryPattern.matcher(line);
            if (matcher.find()) {
                passedTests = Integer.parseInt(matcher.group(1));
                totalTests = Integer.parseInt(matcher.group(2));
                log.info("📊 Summary found: {}/{} tests passed", passedTests, totalTests);
                break;
            }
        }

        // Build final results with ACTUAL INPUTS
        for (int testNum = 1; testNum <= totalTests; testNum++) {
            Map<String, Object> testData = testDataMap.get(testNum);

            // Determine test result using PRIORITY ORDER
            boolean testPassed;

            // Priority 1: Use [DEBUG] Result if available (most reliable)
            if (testData.containsKey("debugResult")) {
                testPassed = (Boolean) testData.get("debugResult");
                log.debug("🎯 Test {}: Using DEBUG result = {}", testNum, testPassed);
            }
            // Priority 2: Use Test Case X: PASS/FAIL status
            else if (testData.containsKey("status")) {
                testPassed = (Boolean) testData.get("status");
                log.debug("🎯 Test {}: Using status result = {}", testNum, testPassed);
            }
            // Priority 3: Compare expected vs actual
            else if (testData.containsKey("expected") && testData.containsKey("actual")) {
                String expected = (String) testData.get("expected");
                String actual = (String) testData.get("actual");
                testPassed = expected.equals(actual);
                log.debug("🎯 Test {}: Comparing '{}' vs '{}' = {}", testNum, expected, actual, testPassed);
            }
            // Priority 4: Use summary count (fallback)
            else {
                testPassed = testNum <= passedTests;
                log.debug("🎯 Test {}: Using summary fallback = {} (passed: {})", testNum, testPassed, passedTests);
            }

            TestCase testCase = testCases.get(testNum - 1);

            Map<String, Object> testResult = new HashMap<>();

            // ✅ CRITICAL FIX: Use actual input from parsed data OR format from TestCase
            String displayInput;
            if (testData.containsKey("actualInput")) {
                displayInput = (String) testData.get("actualInput");
            } else {
                displayInput = formatInputForDisplay(testCase.getInput());
            }

            testResult.put("input", displayInput); // ✅ NOW SHOWS ACTUAL INPUT VALUES
            testResult.put("expectedOutput", testData.getOrDefault("expected", testCase.getExpectedOutput()));
            testResult.put("actualOutput", testData.getOrDefault("actual", testPassed ? "Correct" : "Check your logic"));
            testResult.put("passed", testPassed);
            testResult.put("visible", testCase.getType() == TestCase.Type.PUBLIC);

            results.add(testResult);

            // Log each test result for debugging
            if (!testPassed) {
                log.info("❌ Test Case {}: FAILED - Input: '{}', Expected: '{}', Got: '{}'",
                        testNum, displayInput, testResult.get("expectedOutput"), testResult.get("actualOutput"));
            } else {
                log.info("✅ Test Case {}: PASSED - Input: '{}'", testNum, displayInput);
            }
        }

        int actualPassedCount = (int) results.stream().filter(tr -> (Boolean) tr.get("passed")).count();
        log.info("📊 Final parsing results: {} passed, {} failed", actualPassedCount, totalTests - actualPassedCount);

        return results;
    }

    // ✅ NEW HELPER METHOD: Format input for proper display (like LeetCode)
    // 🎯 ULTIMATE FIX: Format input for display in SubmissionService
    // 🎯 NUCLEAR FIX: Works for ALL services - Copy this EXACT method
    private String formatInputForDisplay(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return "No input";
        }

        String input = rawInput.trim();

        // 🔧 HANDLE MULTI-STRING INPUTS (like "anagram", "nagaram")
        if (input.contains(",") && input.contains("\"")) {
            // Extract all quoted strings and join with space (NO NEWLINES!)
            StringBuilder result = new StringBuilder();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]*)\"");
            java.util.regex.Matcher matcher = pattern.matcher(input);

            boolean first = true;
            while (matcher.find()) {
                if (!first) result.append(" ");  // SPACE, NOT NEWLINE!
                result.append(matcher.group(1));
                first = false;
            }

            return result.toString();  // Returns: "anagram nagaram" (SAFE!)
        }

        // Handle arrays
        if (input.startsWith("[") && input.endsWith("]")) {
            return input;
        }

        // Handle single quoted strings
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }

        return input;
    }

    /**
     * ✅ FIXED: LeetCode-style submission method - NOW TESTS ALL CASES
     */
    @Transactional
    public SubmissionResponse submitSolutionLeetCodeStyle(Long problemId, Integer languageId, String userCode, String username) {
        log.info("🚀 [LEETCODE-STYLE] Starting submission for problem {} by user {}", problemId, username);

        try {
            // Phase 1: Validation
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not supported"));

            // Phase 2: Get ALL test cases (visible + hidden) - CRITICAL FIX
            List<TestCase> allTestCases = testCaseRepository.findByProblemIdOrderBySortOrderAsc(problemId);
            if (allTestCases.isEmpty()) {
                throw new RuntimeException("No test cases found for this problem");
            }

            log.info("📊 Found {} total test cases", allTestCases.size());

            // Phase 3: Generate executable code ONCE for all test cases
            String executableCode;
            try {
                executableCode = codeTemplateService.generateExecutableCode(userCode, problemId, languageId, allTestCases);
                log.info("✅ Generated executable code successfully (length: {})", executableCode.length());
            } catch (Exception e) {
                log.error("❌ Failed to generate executable code: {}", e.getMessage());
                throw new RuntimeException("Failed to prepare code for execution: " + e.getMessage());
            }

            // Phase 4: Execute code with ALL test cases at once
            Map<String, Object> executionResult = executeCodeWithJudge0(executableCode, languageId, "");

            String status = (String) executionResult.get("status");

            // Handle compilation error immediately
            if ("COMPILATION_ERROR".equals(status)) {
                return createCompilationErrorResponse(problemId, problem, language, userCode,
                        (String) executionResult.get("compileOutput"));
            }

            // Parse test results from stdout
            List<Map<String, Object>> testResults = parseTestResults(
                    executionResult.get("stdout") != null ? executionResult.get("stdout").toString() : "",
                    allTestCases
            );

            // Count passed tests
            int passedCount = (int) testResults.stream().filter(tr -> (Boolean) tr.get("passed")).count();
            boolean allTestsPassed = passedCount == allTestCases.size();

            // Phase 5: Create submission record ONLY if all tests passed
            Submission submission = null;
            Long submissionId = -1L;

            if (allTestsPassed) {
                submission = new Submission();
                submission.setUser(user);
                submission.setProblem(problem);
                submission.setLanguage(language);
                submission.setSourceCode(userCode);
                submission.setStatus(Submission.Status.ACCEPTED);
                submission.setCreatedAt(LocalDateTime.now());
                submission.setUpdatedAt(LocalDateTime.now());

                if (executionResult.get("runtimeMs") != null) {
                    submission.setRuntimeMs(((Number) executionResult.get("runtimeMs")).intValue());
                }
                if (executionResult.get("memoryKb") != null) {
                    submission.setMemoryKb(((Number) executionResult.get("memoryKb")).intValue());
                }

                submission = submissionRepository.save(submission);
                submissionId = submission.getId();
                log.info("💾 [LEETCODE-STYLE] Submission {} saved to database", submission.getId());
            } else {
                log.info("🚫 [LEETCODE-STYLE] Submission NOT saved - tests failed");
            }

            // Phase 6: Build response
            SubmissionResponse response = new SubmissionResponse();
            response.setId(submissionId);
            response.setStatus(allTestsPassed ? "ACCEPTED" : "WRONG_ANSWER");
            response.setMessage(allTestsPassed ?
                    "🎉 Congratulations! All test cases passed. Solution accepted!" :
                    String.format("❌ %d out of %d test cases failed. Keep trying!",
                            allTestCases.size() - passedCount, allTestCases.size()));
            response.setPassedTestCases(passedCount);
            response.setTotalTestCases(allTestCases.size());
            response.setProblemId(problemId);
            response.setProblemTitle(problem.getTitle());
            response.setLanguage(language.getDisplayName());
            response.setSourceCode(userCode);
            response.setCreatedAt(submission != null ? submission.getCreatedAt() : LocalDateTime.now());
            response.setSubmissionSaved(allTestsPassed);
            response.setRuntimeMs(executionResult.get("runtimeMs") != null ?
                    ((Number) executionResult.get("runtimeMs")).intValue() : null);
            response.setMemoryKb(executionResult.get("memoryKb") != null ?
                    ((Number) executionResult.get("memoryKb")).intValue() : null);

            return response;

        } catch (Exception e) {
            log.error("❌ [LEETCODE-STYLE] Submission failed: {}", e.getMessage(), e);

            SubmissionResponse errorResponse = new SubmissionResponse();
            errorResponse.setId(-1L);
            errorResponse.setStatus("ERROR");
            errorResponse.setMessage("❌ Submission failed: " + e.getMessage());
            errorResponse.setSubmissionSaved(false);
            errorResponse.setPassedTestCases(0);
            errorResponse.setTotalTestCases(0);
            errorResponse.setCreatedAt(LocalDateTime.now());

            return errorResponse;
        }
    }

    /**
     * ✅ CRITICAL FIX - Execute code with Judge0 using extended timeouts for C++
     */
    private Map<String, Object> executeCodeWithJudge0(String executableCode, Integer languageId, String input) {
        // ✅ CRITICAL FIX - Extended for C++ compilation
        int maxAttempts = 15; // ✅ FIXED - Extended from 30 to 50 for C++ compilation
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            log.info("Judge0 execution attempt {}/{} for language {}", attempt, maxAttempts, languageId);

            try {
                Judge0Service.Judge0SubmissionRequest judge0Request = Judge0Service.Judge0SubmissionRequest.builder()
                        .source_code(executableCode)
                        .language_id(languageId)
                        .stdin(input != null ? input : "")
                        .build();

                Mono<Judge0Service.Judge0SubmissionResponse> submissionMono = judge0Service.submitCode(judge0Request);
                Judge0Service.Judge0SubmissionResponse judge0Response = submissionMono.block();

                if (judge0Response == null || judge0Response.getToken() == null) {
                    String errorMsg = String.format("Judge0 response invalid on attempt %d", attempt);
                    log.error(errorMsg);
                    lastException = new RuntimeException(errorMsg);

                    if (attempt < maxAttempts) {
                        Thread.sleep(2000);
                        continue;
                    }
                    break;
                }

                String token = judge0Response.getToken();
                log.info("Got Judge0 token: {} on attempt {}", token, attempt);

                // ✅ CRITICAL FIX - Extended polling for C++ compilation
                Judge0Service.Judge0ResultResponse result = pollForResultSync(token, 40); // ✅ FIXED - Extended from 30 to 40

                if (result == null) {
                    String errorMsg = String.format("Judge0 result timeout on attempt %d", attempt);
                    log.error(errorMsg);
                    lastException = new RuntimeException(errorMsg);

                    if (attempt < maxAttempts) {
                        Thread.sleep(3000);
                        continue;
                    }
                    break;
                }

                // Process successful result
                String status = "SUCCESS";
                Integer runtimeMs = null;

                if (result.getTime() != null) {
                    try {
                        double timeSeconds = Double.parseDouble(result.getTime());
                        runtimeMs = (int) (timeSeconds * 1000);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse execution time: {}", result.getTime());
                    }
                }

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

                // ✅ CRITICAL FIX - Use HashMap for null safety
                Map<String, Object> response = new HashMap<>();
                response.put("status", status);
                response.put("stdout", result.getStdout() != null ? result.getStdout() : "");
                response.put("stderr", result.getStderr() != null ? result.getStderr() : "");
                response.put("compileOutput", result.getCompile_output() != null ? result.getCompile_output() : "");
                response.put("runtimeMs", runtimeMs != null ? runtimeMs : 0);
                response.put("memoryKb", result.getMemory() != null ? result.getMemory() : 0);

                log.info("Judge0 execution successful on attempt {} - Status: {}", attempt, status);
                return response;

            } catch (Exception e) {
                lastException = e;
                log.error("Judge0 execution failed on attempt {}: {}", attempt, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(Math.min(2000 * attempt, 10000)); // Progressive delay
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All attempts failed
        log.error("All Judge0 execution attempts failed after {} tries", maxAttempts);

        // ✅ CRITICAL FIX - Use HashMap for error responses
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "ERROR");
        errorResult.put("message", "Service temporarily unavailable after " + maxAttempts + " attempts. Please try again later.");
        errorResult.put("lastError", lastException != null ? lastException.getMessage() : "Unknown error");
        errorResult.put("stdout", "");
        errorResult.put("stderr", lastException != null ? lastException.getMessage() : "");
        errorResult.put("compileOutput", "");
        errorResult.put("runtimeMs", 0);
        errorResult.put("memoryKb", 0);

        return errorResult;
    }

    // ✅ ENHANCED - Better status name mapping for debugging
    private String getStatusName(Integer statusId) {
        if (statusId == null) return "UNKNOWN";

        switch (statusId) {
            case 1: return "IN_QUEUE";           // ← Still waiting
            case 2: return "PROCESSING";         // ← Still compiling/running
            case 3: return "ACCEPTED";           // ← Success!
            case 4: return "WRONG_ANSWER";
            case 5: return "TIME_LIMIT_EXCEEDED";
            case 6: return "COMPILATION_ERROR";   // ← C++ compilation failed
            case 7: return "RUNTIME_ERROR_SIGSEGV";
            case 8: return "RUNTIME_ERROR_SIGXFSZ";
            case 9: return "RUNTIME_ERROR_SIGFPE";
            case 10: return "RUNTIME_ERROR_SIGABRT";
            case 11: return "RUNTIME_ERROR_NZEC";
            case 12: return "RUNTIME_ERROR_OTHER";
            case 13: return "INTERNAL_ERROR";
            case 14: return "EXEC_FORMAT_ERROR";
            default: return "RUNTIME_ERROR";
        }
    }

    /**
     * ✅ Create compilation error response
     */
    private SubmissionResponse createCompilationErrorResponse(Long problemId, Problem problem, Language language, String userCode, String compileError) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(-1L);
        response.setStatus("COMPILATION_ERROR");
        response.setProblemId(problemId);
        response.setProblemTitle(problem.getTitle());
        response.setLanguage(language.getDisplayName());
        response.setSourceCode(userCode);
        response.setCreatedAt(LocalDateTime.now());
        response.setSubmissionSaved(false);
        response.setPassedTestCases(0);
        response.setTotalTestCases(0);

        StringBuilder errorMessage = new StringBuilder("❌ Compilation Error:\n\n");
        if (compileError != null && !compileError.trim().isEmpty()) {
            errorMessage.append("Compiler Output:\n").append(compileError).append("\n\n");
        }
        errorMessage.append("💡 Please fix the compilation errors and try again.");

        response.setMessage(errorMessage.toString());
        response.setCompileOutput(compileError);

        return response;
    }

    // ✅ KEEP ALL YOUR OTHER EXISTING METHODS (runCodeDirectly, etc.)

    public Map<String, Object> runCodeDirectlyWithRetry(Integer languageId, String sourceCode, String input) {
        int maxRetries = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🔄 Code execution attempt {} of {}", attempt, maxRetries);
                Map<String, Object> result = runCodeDirectly(languageId, sourceCode, input);

                if (!"ERROR".equals(result.get("status"))) {
                    return result;
                }

                String message = (String) result.get("message");
                if (message != null && (message.contains("Connection reset") ||
                        message.contains("timeout") ||
                        message.contains("temporarily unavailable"))) {
                    lastException = new RuntimeException(message);
                    if (attempt < maxRetries) {
                        log.warn("⚠️ Connection issue detected, retrying in {}s...", attempt);
                        Thread.sleep(attempt * 1000);
                        continue;
                    }
                }

                return result;

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

        // ✅ FIXED - Use HashMap instead of Map.of()
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "ERROR");
        errorResult.put("message", "Service temporarily unavailable after " + maxRetries + " attempts. Please try again later.");
        errorResult.put("lastError", lastException != null ? lastException.getMessage() : "Unknown error");
        errorResult.put("stdout", "");
        errorResult.put("stderr", lastException != null ? lastException.getMessage() : "");
        errorResult.put("compileOutput", "");
        errorResult.put("runtimeMs", 0);
        errorResult.put("memoryKb", 0);
        return errorResult;
    }

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

    public Map<String, Object> runCodeDirectly(Integer languageId, String sourceCode, String input) {
        log.info("Running code directly - Language ID: {}", languageId);
        try {
            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not supported: " + languageId));

            log.info("Language found: {}", language.getDisplayName());

            Judge0Service.Judge0SubmissionRequest judge0Request = Judge0Service.Judge0SubmissionRequest.builder()
                    .source_code(sourceCode)
                    .language_id(languageId)
                    .stdin(input != null ? input : "")  // ✅ CRITICAL FIX - Never send null
                    .build();

            Mono<Judge0Service.Judge0SubmissionResponse> submissionMono = judge0Service.submitCode(judge0Request);
            Judge0Service.Judge0SubmissionResponse judge0Response = submissionMono.block();

            if (judge0Response == null || judge0Response.getToken() == null) {
                log.error("Failed to get token from Judge0");
                // ✅ FIXED - Use HashMap instead of Map.of() for null safety
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "ERROR");
                errorResult.put("message", "Judge0 service temporarily unavailable. Please try again.");
                errorResult.put("stdout", "");
                errorResult.put("stderr", "");
                errorResult.put("compileOutput", "");
                errorResult.put("runtimeMs", 0);
                errorResult.put("memoryKb", 0);
                return errorResult;
            }

            String token = judge0Response.getToken();
            log.info("Got Judge0 token: {}", token);

            // ✅ CRITICAL FIX - Extended timeout for C++ compilation
            Judge0Service.Judge0ResultResponse result = pollForResultSync(token, 40); // ✅ FIXED - Extended from 15 to 40

            if (result == null) {
                // ✅ FIXED - Use HashMap for null safety
                Map<String, Object> timeoutResult = new HashMap<>();
                timeoutResult.put("status", "TIMEOUT");
                timeoutResult.put("message", "Code execution timed out");
                timeoutResult.put("stdout", "");
                timeoutResult.put("stderr", "");
                timeoutResult.put("compileOutput", "");
                timeoutResult.put("runtimeMs", 0);
                timeoutResult.put("memoryKb", 0);
                return timeoutResult;
            }

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

            // ✅ CRITICAL FIX - Use HashMap with null-safe values
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("stdout", result.getStdout() != null ? result.getStdout() : "");
            response.put("stderr", result.getStderr() != null ? result.getStderr() : "");
            response.put("compileOutput", result.getCompile_output() != null ? result.getCompile_output() : "");
            response.put("runtimeMs", runtimeMs != null ? runtimeMs : 0);
            response.put("memoryKb", memoryKb != null ? memoryKb : 0);
            response.put("message", result.getMessage() != null ? result.getMessage() : "");
            response.put("language", language.getDisplayName());
            response.put("executionTime", result.getTime() != null ? result.getTime() : "0");

            log.info("Code execution completed - Status: {}, Runtime: {}ms", status, runtimeMs);
            return response;

        } catch (Exception e) {
            log.error("❌ Error during direct code execution", e);

            String errorMessage = e.getMessage();
            String className = e.getClass().getSimpleName();

            // ✅ CRITICAL FIX - Use HashMap for all error responses
            Map<String, Object> errorResult = new HashMap<>();

            if ("SocketException".equals(className) ||
                    (errorMessage != null && (errorMessage.contains("Connection reset") ||
                            errorMessage.contains("timeout") ||
                            errorMessage.contains("temporarily unavailable")))) {
                errorResult.put("status", "ERROR");
                errorResult.put("message", "Judge0 service temporarily unavailable. Please try again.");
            } else {
                errorResult.put("status", "ERROR");
                errorResult.put("message", "Execution error: " + (errorMessage != null ? errorMessage : "Unknown error"));
            }

            errorResult.put("stdout", "");
            errorResult.put("stderr", errorMessage != null ? errorMessage : "Unknown error");
            errorResult.put("compileOutput", "");
            errorResult.put("runtimeMs", 0);
            errorResult.put("memoryKb", 0);

            return errorResult;
        }
    }

    // ✅ CRITICAL FIX - Extended polling with progressive delays for C++
    private Judge0Service.Judge0ResultResponse pollForResultSync(String token, int maxAttempts) {
        log.info("🔄 Starting polling for token: {} (max attempts: {})", token, maxAttempts);

        for (int i = 0; i < maxAttempts; i++) {
            try {
                // ✅ FIXED - Progressive delay (starts fast, gets slower)
                int delay = Math.min(1000 + (i * 500), 5000); // 1s -> 5s max
                log.debug("⏰ Polling attempt {}/{} - waiting {}ms", i + 1, maxAttempts, delay);
                Thread.sleep(delay);

                Mono<Judge0Service.Judge0ResultResponse> resultMono = judge0Service.getSubmissionResult(token);
                Judge0Service.Judge0ResultResponse result = resultMono.block();

                if (result != null && result.getStatus() != null) {
                    Integer statusId = result.getStatus().getId();
                    String statusName = getStatusName(statusId);
                    log.info("📊 Judge0 polling attempt {}/{}: Status = {} ({})",
                            i + 1, maxAttempts, statusId, statusName);

                    // ✅ CRITICAL - Check if processing is complete
                    if (statusId != null && statusId != 1 && statusId != 2) {
                        log.info("✅ Execution completed after {} attempts - Final status: {}",
                                i + 1, statusName);
                        return result;
                    }

                    // Log progress for long-running compilations
                    if (i > 5 && (statusId == 1 || statusId == 2)) {
                        log.info("⏳ Still processing... (attempt {}/{}) - C++ compilation may take time",
                                i + 1, maxAttempts);
                    }
                } else {
                    log.warn("⚠️ Null result received on attempt {}", i + 1);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("❌ Polling interrupted on attempt {}", i + 1);
                break;
            } catch (Exception e) {
                log.error("❌ Error during polling attempt {}: {}", i + 1, e.getMessage());

                // ✅ CRITICAL - Don't break immediately for network issues
                if (i < maxAttempts - 3) { // Give more chances for network issues
                    continue;
                }
                break;
            }
        }

        log.error("❌ Polling timeout after {} attempts for token: {}", maxAttempts, token);
        return null;
    }

    // ✅ KEEP ALL YOUR OTHER HELPER METHODS (mapToSubmissionResponse, etc.)
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

    public Map<String, Object> getProblemTemplate(Long problemId, Integer languageId) {
        try {
            String template = codeTemplateService.getTemplate(problemId, languageId);
            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not found"));
            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            List<TestCase> visibleTestCases = testCaseRepository.findVisibleTestCasesByProblemId(problemId);

            // ✅ FIXED - Use HashMap instead of Map.of()
            Map<String, Object> templateResult = new HashMap<>();
            templateResult.put("template", template);
            templateResult.put("language", language.getDisplayName());
            templateResult.put("imports", codeTemplateService.getImports(language.getDisplayName().toLowerCase()));
            templateResult.put("problemTitle", problem.getTitle());
            templateResult.put("functionName", problem.getFunctionName() != null ? problem.getFunctionName() : "solution");
            templateResult.put("returnType", problem.getReturnType() != null ? problem.getReturnType() : "int");
            templateResult.put("usesTemplate", problem.isTemplateEnabled());
            templateResult.put("exampleTestCases", visibleTestCases.stream()
                    .limit(3)
                    .map(tc -> {
                        Map<String, Object> tcMap = new HashMap<>();
                        tcMap.put("input", tc.getInput());
                        tcMap.put("output", tc.getExpectedOutput());
                        return tcMap;
                    })
                    .collect(Collectors.toList()));

            return templateResult;

        } catch (Exception e) {
            log.error("Error getting problem template: {}", e.getMessage());

            // ✅ FIXED - Use HashMap instead of Map.of()
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("template", codeTemplateService.getDefaultTemplate(languageId));
            errorResult.put("language", "java");
            errorResult.put("imports", "");
            errorResult.put("problemTitle", "Problem " + problemId);
            errorResult.put("functionName", "solution");
            errorResult.put("returnType", "int");
            errorResult.put("usesTemplate", true);
            errorResult.put("exampleTestCases", List.of());
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
}
