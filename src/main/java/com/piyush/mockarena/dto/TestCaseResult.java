package com.piyush.mockarena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private Integer caseNumber; // ✅ Your existing field
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private Boolean passed;
    private String status;
    private Integer runtimeMs;
    private String errorMessage;

    // ✅ ADDED: Additional LeetCode-style fields
    private Boolean visible = true; // Whether test case is shown to user (public vs hidden)
    private Integer memoryKb; // Memory usage
}
