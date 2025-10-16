package com.piyush.mockarena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionResponse {
    private Boolean success;
    private String status; // ACCEPTED, WRONG_ANSWER, COMPILE_ERROR, etc.
    private String message;

    // Test case results
    private List<TestCaseResult> testCaseResults;
    private Integer totalTestCases;
    private Integer passedTestCases;
    private Boolean allTestsPassed;

    // Performance metrics
    private Integer executionTimeMs;
    private Integer memoryUsedKb;

    // Submission info
    private Long submissionId;
    private Boolean isSubmission = false;

    // Debug information (only for failed submissions)
    private String stdout;
    private String stderr;
    private String compileOutput;
    private String errorDetails;

    // Additional metadata
    private Double acceptanceRate;
    private String problemTitle;
}
