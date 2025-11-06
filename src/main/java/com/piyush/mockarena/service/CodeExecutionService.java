package com.piyush.mockarena.service;

import com.piyush.mockarena.entity.*;
import com.piyush.mockarena.repository.*;
import com.piyush.mockarena.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CodeExecutionService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private CodeTemplateService codeTemplateService;

    @Autowired
    private Judge0Service judge0Service;

    /**
     * 🚀 UNIVERSAL CODE EXECUTION - Handles both Run and Submit (FIXED FOR ALL PROBLEMS)
     */
    @Async("submissionExecutor")
    public CompletableFuture<CodeExecutionResponse> executeCode(
            CodeRunRequest request, String username, boolean isSubmission) {

        try {
            log.info("🔥 Starting code execution - Problem: {}, User: {}, Submission: {}",
                    request.getProblemId(), username, isSubmission);

            // 1. Validate inputs
            Problem problem = problemRepository.findById(request.getProblemId())
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            Language language = languageRepository.findById(request.getLanguageId())
                    .orElseThrow(() -> new RuntimeException("Language not found"));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Get test cases based on execution type
            List<TestCase> testCases = getTestCases(problem, isSubmission);
            log.info("📋 Retrieved {} test cases for execution", testCases.size());

            // 3. 🎯 FIXED: Generate LeetCode-style executable code directly
            String executableCode = codeTemplateService.generateExecutableCode(
                    request.getSourceCode(),
                    request.getProblemId(),
                    request.getLanguageId(),
                    testCases
            );
            log.info("🔧 Generated executable code for LeetCode-style execution");

            // 4. Create submission record if needed
            Submission submission = null;
            if (isSubmission) {
                submission = createSubmissionRecord(user, problem, language, request.getSourceCode());
            }

            // 5. Execute code against test cases
            List<TestCaseResult> results = new ArrayList<>();
            int passedCount = 0;
            boolean allPassed = true;
            String overallStatus = "ACCEPTED";
            int totalRuntime = 0;
            int maxMemory = 0;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                log.info("🧪 Executing test case {} of {}", i + 1, testCases.size());

                try {
                    // Execute single test case using executable code
                    TestCaseResult result = executeSingleTestCase(executableCode, testCase, language, i + 1);
                    results.add(result);

                    if (result.getPassed()) {
                        passedCount++;
                    } else {
                        allPassed = false;
                        if ("ACCEPTED".equals(overallStatus)) {
                            overallStatus = determineFailureStatus(result);
                        }
                    }

                    // Aggregate performance metrics
                    if (result.getRuntimeMs() != null) {
                        totalRuntime += result.getRuntimeMs();
                    }
                    if (result.getMemoryKb() != null && result.getMemoryKb() > maxMemory) {
                        maxMemory = result.getMemoryKb();
                    }

                    // For non-submissions, stop on first failure to save resources
                    if (!isSubmission && !result.getPassed()) {
                        log.info("🚫 Stopping execution on first failure (Run mode)");
                        break;
                    }

                } catch (Exception e) {
                    log.error("❌ Test case {} execution failed: {}", i + 1, e.getMessage());
                    TestCaseResult errorResult = TestCaseResult.builder()
                            .caseNumber(i + 1)
                            .input(testCase.getInput())
                            .expectedOutput(testCase.getExpectedOutput())
                            .actualOutput("Runtime Error")
                            .passed(false)
                            .status("RUNTIME_ERROR")
                            .errorMessage(e.getMessage())
                            .visible(testCase.getType() == TestCase.Type.PUBLIC)
                            .build();
                    results.add(errorResult);
                    allPassed = false;
                    overallStatus = "RUNTIME_ERROR";
                }
            }

            // 6. Update submission record
            if (submission != null) {
                updateSubmissionRecord(submission, results, overallStatus, totalRuntime, maxMemory);
            }

            // 7. Build response
            CodeExecutionResponse response = buildExecutionResponse(
                    problem, results, passedCount, testCases.size(), allPassed,
                    overallStatus, totalRuntime, maxMemory, submission, isSubmission);

            log.info("✅ Code execution completed - Status: {}, Passed: {}/{}",
                    overallStatus, passedCount, testCases.size());

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("💥 Code execution failed: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    CodeExecutionResponse.builder()
                            .success(false)
                            .status("INTERNAL_ERROR")
                            .message("Execution failed: " + e.getMessage())
                            .allTestsPassed(false)
                            .isSubmission(isSubmission)
                            .build()
            );
        }
    }

    /**
     * 🔧 TEMPLATE-BASED EXECUTION - LeetCode style function completion (OPTIMIZED)
     */
    @Async("submissionExecutor")
    public CompletableFuture<CodeExecutionResponse> executeWithTemplate(
            CodeRunWithTemplateRequest request, String username, boolean isSubmission) {

        try {
            log.info("🔧 Starting template-based execution - Problem: {}, User: {}",
                    request.getProblemId(), username);

            // Convert to standard execution request directly
            CodeRunRequest standardRequest = new CodeRunRequest();
            standardRequest.setProblemId(request.getProblemId());
            standardRequest.setLanguageId(request.getLanguageId());
            standardRequest.setSourceCode(request.getUserCode()); // Use user code directly
            standardRequest.setIsSubmission(isSubmission);

            // Execute using standard flow (will auto-generate executable code)
            return executeCode(standardRequest, username, isSubmission);

        } catch (Exception e) {
            log.error("💥 Template execution failed: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    CodeExecutionResponse.builder()
                            .success(false)
                            .status("TEMPLATE_ERROR")
                            .message("Template generation failed: " + e.getMessage())
                            .allTestsPassed(false)
                            .isSubmission(isSubmission)
                            .build()
            );
        }
    }

    /**
     * 📋 Get appropriate test cases based on execution type
     */
    private List<TestCase> getTestCases(Problem problem, boolean isSubmission) {
        if (isSubmission) {
            // Full submission: all test cases
            return testCaseRepository.findByProblemAndIsActiveTrueOrderBySortOrder(problem);
        } else {
            // Run mode: only public (visible) test cases
            return testCaseRepository.findByProblemAndTypeAndIsActiveTrueOrderBySortOrder(
                    problem, TestCase.Type.PUBLIC);
        }
    }

    /**
     * 🧪 Execute single test case using Judge0 (COMPLETELY FIXED FOR ALL PROBLEMS)
     */
    private TestCaseResult executeSingleTestCase(String code, TestCase testCase,
                                                 Language language, int caseNumber) {
        try {
            // Create Judge0 request - NO STDIN for LeetCode-style problems
            Judge0Service.Judge0SubmissionRequest judge0Request = Judge0Service.Judge0SubmissionRequest.builder()
                    .source_code(code)
                    .language_id(language.getId())
                    .stdin("") // Empty stdin for LeetCode-style problems
                    .expected_output(testCase.getExpectedOutput().trim())
                    .cpu_time_limit(2.0f)
                    .memory_limit(256000) // 256MB in KB
                    .build();

            // Execute and get result
            Judge0Service.Judge0SubmissionResponse judge0Response = judge0Service.submitCode(judge0Request)
                    .block(); // Blocking call for synchronous execution

            if (judge0Response == null) {
                throw new RuntimeException("Judge0 returned null response");
            }

            // Parse results - look for test case results in stdout
            String output = judge0Response.getStdout() != null ?
                    judge0Response.getStdout().trim() : "";

            log.info("🎯 Judge0 stdout for case {}: '{}'", caseNumber, output);

            // Extract actual test case result from the test runner output
            String testResult = extractTestCaseOutput(output, caseNumber);
            String expectedOutput = testCase.getExpectedOutput().trim();

            // 🔧 UNIVERSAL RESULT HANDLING - Works for ALL problem types
            boolean passed = false;
            String actualOutput = "";

            if ("PASS".equals(testResult)) {
                // Test runner reported success
                passed = true;
                actualOutput = expectedOutput; // Show expected value for passed tests
                log.info("✅ Test case {} PASSED", caseNumber);
            } else if ("FAIL".equals(testResult)) {
                // Test runner reported failure
                passed = false;
                actualOutput = "Failed";
                log.info("❌ Test case {} FAILED", caseNumber);
            } else if ("Unable to determine result".equals(testResult) || testResult.isEmpty()) {
                // Couldn't parse result - check if all tests in summary passed
                if (output.contains("Results:") && output.contains("test cases passed")) {
                    // Try to infer from overall results
                    if (inferTestCasePassed(output, caseNumber)) {
                        passed = true;
                        actualOutput = expectedOutput;
                        log.info("✅ Test case {} inferred as PASSED from summary", caseNumber);
                    } else {
                        passed = false;
                        actualOutput = "Failed";
                        log.info("❌ Test case {} inferred as FAILED from summary", caseNumber);
                    }
                } else {
                    // Default to failed if we can't determine
                    passed = false;
                    actualOutput = "Unable to determine";
                    log.warn("⚠️ Could not determine result for test case {}", caseNumber);
                }
            } else {
                // Direct value comparison (for cases where actual value is returned)
                passed = testResult.equals(expectedOutput);
                actualOutput = testResult;
                log.info("🔍 Direct comparison for case {}: Expected='{}', Actual='{}', Passed={}",
                        caseNumber, expectedOutput, testResult, passed);
            }

            String status = determineTestCaseStatus(judge0Response, passed);

            return TestCaseResult.builder()
                    .caseNumber(caseNumber)
                    .input(testCase.getInput())
                    .expectedOutput(expectedOutput)
                    .actualOutput(actualOutput)
                    .passed(passed)
                    .status(status)
                    .runtimeMs(judge0Response.getTime() != null ?
                            Math.round(judge0Response.getTime() * 1000) : null)
                    .memoryKb(judge0Response.getMemory())
                    .visible(testCase.getType() == TestCase.Type.PUBLIC)
                    .errorMessage(judge0Response.getStderr())
                    .build();

        } catch (Exception e) {
            log.error("❌ Single test case execution failed: {}", e.getMessage());
            return TestCaseResult.builder()
                    .caseNumber(caseNumber)
                    .input(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput("Runtime Error: " + e.getMessage())
                    .passed(false)
                    .status("RUNTIME_ERROR")
                    .errorMessage(e.getMessage())
                    .visible(testCase.getType() == TestCase.Type.PUBLIC)
                    .build();
        }
    }

    /**
     * 🎯 UNIVERSAL OUTPUT EXTRACTION - Works for ALL languages and problem types
     */
    /**
     * 🎯 ENHANCED OUTPUT EXTRACTION with Python Debug Support
     */
    private String extractTestCaseOutput(String fullOutput, int caseNumber) {
        if (fullOutput == null || fullOutput.isEmpty()) {
            return "Unable to determine result";
        }

        log.info("🔍 Extracting output for test case {} from: '{}'", caseNumber, fullOutput);

        String[] lines = fullOutput.split("\n");

        // NEW: Look for debug output first
        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("[DEBUG] Test Case " + caseNumber + " - Result:")) {
                String result = trimmedLine.substring(trimmedLine.indexOf("Result: ") + 8).trim();
                log.info("🐛 Found debug result for case {}: '{}'", caseNumber, result);
                return result;
            }
        }

        // Look for explicit PASS/FAIL indicators
        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.contains("Test Case " + caseNumber + ": PASS")) {
                log.info("✅ Found explicit PASS for test case {}", caseNumber);
                return "PASS";
            }
            if (trimmedLine.contains("Test Case " + caseNumber + ": FAIL")) {
                log.info("❌ Found explicit FAIL for test case {}", caseNumber);
                return "FAIL";
            }
        }

        // Look for Output: lines
        boolean inTestCase = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.contains("Test Case " + caseNumber)) {
                inTestCase = true;
                continue;
            }

            if (inTestCase) {
                if (line.startsWith("Output: ")) {
                    String output = line.substring("Output: ".length()).trim();
                    log.info("✅ Found output for case {}: '{}'", caseNumber, output);
                    return output;
                }

                if (line.startsWith("Actual: ")) {
                    String actual = line.substring("Actual: ".length()).trim();
                    log.info("✅ Found actual for case {}: '{}'", caseNumber, actual);
                    return actual;
                }

                // Stop if we hit next test case
                if (line.contains("Test Case " + (caseNumber + 1))) {
                    break;
                }
            }
        }

        // Try to infer from summary
        if (fullOutput.contains("Results:") && fullOutput.contains("test cases passed")) {
            if (inferTestCasePassed(fullOutput, caseNumber)) {
                return "PASS";
            } else {
                return "FAIL";
            }
        }

        log.warn("⚠️ Could not extract result for test case {}", caseNumber);
        return "Unable to determine result";
    }


    /**
     * 🎯 INFER TEST CASE RESULT from overall summary
     */
    private boolean inferTestCasePassed(String output, int caseNumber) {
        try {
            // Look for patterns like "Results: 2/2 test cases passed"
            if (output.contains("Results:") && output.contains("test cases passed")) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.contains("Results:") && line.contains("/") && line.contains("test cases passed")) {
                        // Extract "2/2" part
                        String resultPart = line.substring(line.indexOf("Results:") + 8);
                        resultPart = resultPart.substring(0, resultPart.indexOf("test cases passed")).trim();

                        if (resultPart.contains("/")) {
                            String[] parts = resultPart.split("/");
                            if (parts.length == 2) {
                                int passed = Integer.parseInt(parts[0].trim());
                                int total = Integer.parseInt(parts[1].trim());

                                // If all tests passed and this test case is within range, assume passed
                                if (passed == total && caseNumber <= total) {
                                    log.info("✅ All {}/{} tests passed, inferring case {} as PASSED", passed, total, caseNumber);
                                    return true;
                                } else {
                                    log.info("❌ Only {}/{} tests passed, inferring case {} as FAILED", passed, total, caseNumber);
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not parse summary results: {}", e.getMessage());
        }

        // Default to failed if we can't determine
        return false;
    }

    /**
     * 📊 Determine test case status based on Judge0 response
     */
    private String determineTestCaseStatus(Judge0Service.Judge0SubmissionResponse response, boolean passed) {
        if (response.getStatus() == null) {
            return "UNKNOWN";
        }

        switch (response.getStatus().getId()) {
            case 3: // Accepted
                return passed ? "ACCEPTED" : "WRONG_ANSWER";
            case 4: // Wrong Answer
                return "WRONG_ANSWER";
            case 5: // Time Limit Exceeded
                return "TIME_LIMIT_EXCEEDED";
            case 6: // Compilation Error
                return "COMPILATION_ERROR";
            case 7: // Runtime Error (SIGSEGV)
            case 8: // Runtime Error (SIGXFSZ)
            case 9: // Runtime Error (SIGFPE)
            case 10: // Runtime Error (SIGABRT)
            case 11: // Runtime Error (NZEC)
            case 12: // Runtime Error (Other)
                return "RUNTIME_ERROR";
            case 13: // Internal Error
                return "INTERNAL_ERROR";
            case 14: // Exec Format Error
                return "RUNTIME_ERROR";
            default:
                return passed ? "ACCEPTED" : "WRONG_ANSWER";
        }
    }

    /**
     * 📝 Create submission record
     */
    private Submission createSubmissionRecord(User user, Problem problem, Language language, String sourceCode) {
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(language);
        submission.setSourceCode(sourceCode);
        submission.setStatus(Submission.Status.PENDING);
        submission.setCreatedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    /**
     * 🔄 Update submission record with results
     */
    private void updateSubmissionRecord(Submission submission, List<TestCaseResult> results,
                                        String status, int totalRuntime, int maxMemory) {
        submission.setStatus(Submission.Status.valueOf(status));
        submission.setRuntimeMs(totalRuntime);
        submission.setMemoryKb(maxMemory);
        submission.setTestCasesPassed(results.stream()
                .mapToInt(r -> r.getPassed() ? 1 : 0).sum());
        submission.setTestCasesTotal(results.size());
        submission.setUpdatedAt(LocalDateTime.now());

        submissionRepository.save(submission);
    }

    /**
     * 🏗️ Build execution response
     */
    private CodeExecutionResponse buildExecutionResponse(Problem problem, List<TestCaseResult> results,
                                                         int passedCount, int totalCount, boolean allPassed,
                                                         String status, int totalRuntime, int maxMemory,
                                                         Submission submission, boolean isSubmission) {
        return CodeExecutionResponse.builder()
                .success(true)
                .status(status)
                .message(generateStatusMessage(status, passedCount, totalCount))
                .testCaseResults(results)
                .totalTestCases(totalCount)
                .passedTestCases(passedCount)
                .allTestsPassed(allPassed)
                .executionTimeMs(totalRuntime)
                .memoryUsedKb(maxMemory)
                .submissionId(submission != null ? submission.getId() : null)
                .isSubmission(isSubmission)
                .problemTitle(problem.getTitle())
                .acceptanceRate(calculateAcceptanceRate(problem))
                .build();
    }

    /**
     * 📊 Calculate problem acceptance rate
     */
    private Double calculateAcceptanceRate(Problem problem) {
        long totalSubmissions = submissionRepository.countByProblem(problem);
        long acceptedSubmissions = submissionRepository.countByProblemAndStatus(
                problem, Submission.Status.ACCEPTED);

        return totalSubmissions > 0 ?
                (double) acceptedSubmissions / totalSubmissions * 100 : 0.0;
    }

    /**
     * 💬 Generate status message
     */
    private String generateStatusMessage(String status, int passed, int total) {
        switch (status) {
            case "ACCEPTED":
                return String.format("✅ Accepted! All %d test cases passed.", total);
            case "WRONG_ANSWER":
                return String.format("❌ Wrong Answer. %d of %d test cases passed.", passed, total);
            case "TIME_LIMIT_EXCEEDED":
                return "⏰ Time Limit Exceeded. Your solution is too slow.";
            case "COMPILATION_ERROR":
                return "🔧 Compilation Error. Please check your syntax.";
            case "RUNTIME_ERROR":
                return "💥 Runtime Error. Your code crashed during execution.";
            default:
                return String.format("Status: %s. %d of %d test cases passed.", status, passed, total);
        }
    }

    /**
     * 🔍 Determine failure status from test case result
     */
    private String determineFailureStatus(TestCaseResult result) {
        return result.getStatus() != null ? result.getStatus() : "WRONG_ANSWER";
    }
}
