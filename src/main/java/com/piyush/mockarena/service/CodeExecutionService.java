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
     * 🚀 EXECUTE CODE: Universal method for both run and submit
     */
    @Async
    public CompletableFuture<CodeExecutionResponse> executeCode(CodeRunRequest request, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🏃 Executing code for user: {} on problem: {} (submission: {})",
                        username, request.getProblemId(), request.getIsSubmission());

                // Get problem and validate
                Problem problem = problemRepository.findById(request.getProblemId())
                        .orElseThrow(() -> new RuntimeException("Problem not found: " + request.getProblemId()));

                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found: " + username));

                Language language = languageRepository.findById(request.getLanguageId())
                        .orElseThrow(() -> new RuntimeException("Language not found: " + request.getLanguageId()));

                // Get appropriate test cases using YOUR EXISTING METHODS
                List<TestCase> testCases = getTestCases(problem, request.getIsSubmission());

                if (testCases.isEmpty()) {
                    throw new RuntimeException("No test cases found for this problem");
                }

                // Generate executable code
                String executableCode = codeTemplateService.generateExecutableCode(
                        request.getSourceCode(),
                        request.getProblemId(),
                        request.getLanguageId(),
                        testCases
                );

                log.debug("Generated executable code for problem {}", request.getProblemId());

                // Execute with Judge0
                Judge0ExecutionResult judge0Result = executeWithJudge0(executableCode, request.getLanguageId());

                // Parse test case results
                List<TestCaseResult> testResults = parseTestResults(
                        judge0Result.getStdout(),
                        judge0Result.getStderr(),
                        testCases
                );

                // Calculate statistics
                int passedCount = (int) testResults.stream()
                        .mapToLong(tr -> Boolean.TRUE.equals(tr.getPassed()) ? 1 : 0)
                        .sum();

                boolean allPassed = passedCount == testCases.size();
                String status = determineStatus(judge0Result, allPassed);

                // Save submission if needed
                Submission submission = null;
                if (Boolean.TRUE.equals(request.getIsSubmission())) {
                    submission = saveSubmission(user, problem, language, request.getSourceCode(),
                            status, judge0Result, testResults);
                    updateProblemStatistics(problem, allPassed);
                }

                // Build response
                return CodeExecutionResponse.builder()
                        .success(judge0Result.isSuccess())
                        .status(status)
                        .message(allPassed ? "All test cases passed!" :
                                passedCount + " out of " + testCases.size() + " test cases passed")
                        .testCaseResults(testResults)
                        .totalTestCases(testCases.size())
                        .passedTestCases(passedCount)
                        .allTestsPassed(allPassed)
                        .executionTimeMs(judge0Result.getExecutionTimeMs())
                        .memoryUsedKb(judge0Result.getMemoryUsedKb())
                        .isSubmission(request.getIsSubmission())
                        .submissionId(submission != null ? submission.getId() : null)
                        .problemTitle(problem.getTitle())
                        .acceptanceRate(problem.getAcceptanceRate())
                        .stdout(!allPassed ? judge0Result.getStdout() : null)
                        .stderr(!allPassed ? judge0Result.getStderr() : null)
                        .compileOutput(!allPassed ? judge0Result.getCompileOutput() : null)
                        .build();

            } catch (Exception e) {
                log.error("❌ Code execution failed", e);
                return CodeExecutionResponse.builder()
                        .success(false)
                        .status("ERROR")
                        .message("Execution failed: " + e.getMessage())
                        .build();
            }
        });
    }

    /**
     * 🔍 GET TEST CASES: Get appropriate test cases using YOUR existing repository methods
     */
    private List<TestCase> getTestCases(Problem problem, Boolean isSubmission) {
        if (Boolean.TRUE.equals(isSubmission)) {
            // For submissions: get ALL active test cases (both PUBLIC and HIDDEN)
            return testCaseRepository.findByProblemAndIsActiveTrueOrderBySortOrder(problem);
        } else {
            // For runs: get only PUBLIC test cases (visible ones), limit to first 3
            List<TestCase> publicTestCases = testCaseRepository
                    .findByProblemAndTypeAndIsActiveTrueOrderBySortOrder(problem, TestCase.Type.PUBLIC);

            // If no PUBLIC test cases exist, fall back to first 3 active test cases
            if (publicTestCases.isEmpty()) {
                List<TestCase> allTestCases = testCaseRepository
                        .findByProblemAndIsActiveTrueOrderBySortOrder(problem);
                return allTestCases.stream().limit(3).collect(Collectors.toList());
            }

            // Return first 3 public test cases
            return publicTestCases.stream().limit(3).collect(Collectors.toList());
        }
    }

    /**
     * ⚡ EXECUTE WITH JUDGE0: Core execution logic (FIXED TYPE HANDLING)
     */
    private Judge0ExecutionResult executeWithJudge0(String sourceCode, Integer languageId) {
        try {
            // Create and submit Judge0 request
            Judge0Service.Judge0SubmissionRequest request =
                    Judge0Service.Judge0SubmissionRequest.builder()
                            .source_code(sourceCode)
                            .language_id(languageId)
                            .stdin("")
                            .build();

            // Submit to Judge0
            Judge0Service.Judge0SubmissionResponse submitResponse = judge0Service.submitCode(request).block();
            if (submitResponse == null || submitResponse.getToken() == null) {
                throw new RuntimeException("Failed to submit code to Judge0");
            }

            String token = submitResponse.getToken();
            log.debug("Code submitted to Judge0 with token: {}", token);

            // Poll for result
            Judge0Service.Judge0ResultResponse result = pollForResult(token);

            // ✅ FIXED: Handle String time and Integer memory correctly
            int executionTimeMs = 0;
            if (result.getTime() != null && !result.getTime().trim().isEmpty()) {
                try {
                    double timeInSeconds = Double.parseDouble(result.getTime());
                    executionTimeMs = (int) (timeInSeconds * 1000); // Convert seconds to ms
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse execution time: {}", result.getTime());
                    executionTimeMs = 0;
                }
            }

            int memoryUsedKb = 0;
            if (result.getMemory() != null) {
                memoryUsedKb = result.getMemory();
            }

            // Convert to our format
            return Judge0ExecutionResult.builder()
                    .success(result.getStatus() != null && result.getStatus().getId() == 3) // 3 = Accepted
                    .statusId(result.getStatus() != null ? result.getStatus().getId() : -1)
                    .stdout(result.getStdout())
                    .stderr(result.getStderr())
                    .compileOutput(result.getCompile_output())
                    .executionTimeMs(executionTimeMs)
                    .memoryUsedKb(memoryUsedKb)
                    .build();

        } catch (Exception e) {
            log.error("Judge0 execution failed", e);
            return Judge0ExecutionResult.builder()
                    .success(false)
                    .statusId(-1)
                    .stdout("")
                    .stderr(e.getMessage())
                    .compileOutput("")
                    .executionTimeMs(0)
                    .memoryUsedKb(0)
                    .build();
        }
    }

    /**
     * ⏱️ POLL FOR RESULT: Wait for Judge0 result
     */
    private Judge0Service.Judge0ResultResponse pollForResult(String token) throws Exception {
        int attempts = 0;
        int maxAttempts = 20; // 40 seconds timeout

        while (attempts < maxAttempts) {
            Thread.sleep(2000); // Wait 2 seconds

            Judge0Service.Judge0ResultResponse result = judge0Service.getSubmissionResult(token).block();
            if (result != null && result.getStatus() != null) {
                Integer statusId = result.getStatus().getId();
                // Stop if not queued (1) or processing (2)
                if (statusId != 1 && statusId != 2) {
                    return result;
                }
            }
            attempts++;
        }

        throw new RuntimeException("Execution timeout - Judge0 did not respond in time");
    }

    /**
     * 🔍 PARSE TEST RESULTS: Parse execution output to extract test results
     */
    private List<TestCaseResult> parseTestResults(String stdout, String stderr, List<TestCase> testCases) {
        List<TestCaseResult> results = new ArrayList<>();

        if (stdout == null || stdout.trim().isEmpty()) {
            // No output - create failed results
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                results.add(TestCaseResult.builder()
                        .caseNumber(i + 1)
                        .input(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .actualOutput("No output")
                        .passed(false)
                        .status("FAILED")
                        .runtimeMs(0)
                        .errorMessage(stderr != null && !stderr.trim().isEmpty() ? stderr : "No output generated")
                        .visible(testCase.getType() == null || testCase.getType() == TestCase.Type.PUBLIC)
                        .build());
            }
            return results;
        }

        // Parse test results from stdout
        Map<Integer, Boolean> testStatuses = extractTestStatuses(stdout);
        Map<Integer, String> actualOutputs = extractActualOutputs(stdout);
        Map<Integer, Integer> runtimes = extractRuntimes(stdout);

        // Create results
        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            int caseNumber = i + 1;
            boolean passed = testStatuses.getOrDefault(caseNumber, false);

            results.add(TestCaseResult.builder()
                    .caseNumber(caseNumber)
                    .input(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput(actualOutputs.getOrDefault(caseNumber, passed ? testCase.getExpectedOutput() : "Failed"))
                    .passed(passed)
                    .status(passed ? "PASSED" : "FAILED")
                    .runtimeMs(runtimes.getOrDefault(caseNumber, 0))
                    .errorMessage(passed ? null : "Test case failed")
                    .visible(testCase.getType() == null || testCase.getType() == TestCase.Type.PUBLIC)
                    .build());
        }

        return results;
    }

    /**
     * 🔧 UTILITY METHODS
     */
    private Map<Integer, Boolean> extractTestStatuses(String stdout) {
        Map<Integer, Boolean> statuses = new HashMap<>();
        String[] lines = stdout.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.matches(".*Test Case \\d+: (PASS|FAIL)")) {
                try {
                    String[] parts = line.split(":");
                    String testPart = parts[0];
                    String statusPart = parts[1].trim();

                    // Extract test case number
                    String numberStr = testPart.replaceAll(".*Test Case (\\d+).*", "$1");
                    int testNumber = Integer.parseInt(numberStr);
                    boolean passed = "PASS".equals(statusPart);

                    statuses.put(testNumber, passed);
                } catch (Exception e) {
                    log.debug("Failed to parse test status from line: {}", line);
                }
            }
        }

        return statuses;
    }

    private Map<Integer, String> extractActualOutputs(String stdout) {
        // Implementation to extract actual outputs if available
        return new HashMap<>();
    }

    private Map<Integer, Integer> extractRuntimes(String stdout) {
        // Implementation to extract individual test case runtimes if available
        return new HashMap<>();
    }

    private String determineStatus(Judge0ExecutionResult result, boolean allPassed) {
        if (!result.isSuccess()) {
            return switch (result.getStatusId()) {
                case 6 -> "COMPILATION_ERROR";
                case 5 -> "TIME_LIMIT_EXCEEDED";
                case 7, 8, 9, 10, 11, 12, 13 -> "RUNTIME_ERROR";
                default -> "ERROR";
            };
        }
        return allPassed ? "ACCEPTED" : "WRONG_ANSWER";
    }

    private Submission saveSubmission(User user, Problem problem, Language language,
                                      String sourceCode, String status, Judge0ExecutionResult result,
                                      List<TestCaseResult> testResults) {

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(language);
        submission.setSourceCode(sourceCode);
        submission.setStatus(Submission.Status.valueOf(status));
        submission.setStdout(result.getStdout());
        submission.setStderr(result.getStderr());
        submission.setCompileOutput(result.getCompileOutput());
        submission.setRuntimeMs(result.getExecutionTimeMs());
        submission.setMemoryKb(result.getMemoryUsedKb());
        submission.setCreatedAt(LocalDateTime.now());
        submission.setUpdatedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    private void updateProblemStatistics(Problem problem, boolean accepted) {
        problem.setTotalSubmissions(problem.getTotalSubmissions() + 1);
        if (accepted) {
            problem.setAcceptedSubmissions(problem.getAcceptedSubmissions() + 1);
        }

        double rate = problem.getTotalSubmissions() > 0 ?
                (double) problem.getAcceptedSubmissions() / problem.getTotalSubmissions() * 100 : 0;
        problem.setAcceptanceRate(rate);

        problemRepository.save(problem);
    }

    public CodeExecutionResponse getSubmissionDetails(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        return CodeExecutionResponse.builder()
                .success(submission.getStatus() == Submission.Status.ACCEPTED)
                .status(submission.getStatus().name())
                .submissionId(submission.getId())
                .executionTimeMs(submission.getRuntimeMs())
                .memoryUsedKb(submission.getMemoryKb())
                .problemTitle(submission.getProblem().getTitle())
                .build();
    }

    /**
     * 🏗️ HELPER CLASS: Judge0 execution result
     */
    @lombok.Data
    @lombok.Builder
    private static class Judge0ExecutionResult {
        private boolean success;
        private int statusId;
        private String stdout;
        private String stderr;
        private String compileOutput;
        private int executionTimeMs;
        private int memoryUsedKb;
    }
}
