package com.piyush.mockarena.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionWithTemplateRequest {
    @NotNull
    private Long problemId;

    @NotNull
    private Integer languageId;

    @NotBlank
    private String userCode; // Only the function implementation
}
