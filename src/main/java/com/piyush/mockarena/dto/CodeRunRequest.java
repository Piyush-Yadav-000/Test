package com.piyush.mockarena.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodeRunRequest {
    @NotNull(message = "Problem ID is required")
    private Long problemId; // ✅ ADDED: Problem ID for test case lookup

    @NotNull(message = "Language ID is required")
    private Integer languageId;

    @NotBlank(message = "Source code is required")
    private String sourceCode;

    private String input; // Custom input for testing (optional)

    // For distinguishing between run and submit
    private Boolean isSubmission = false;
}
