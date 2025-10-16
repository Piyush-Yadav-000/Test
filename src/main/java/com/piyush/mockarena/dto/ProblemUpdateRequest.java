// ProblemUpdateRequest.java
package com.piyush.mockarena.dto;

import com.piyush.mockarena.entity.Problem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ProblemUpdateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private String sampleInput;
    private String sampleOutput;
    private String explanation;

    @NotNull(message = "Difficulty is required")
    private Problem.Difficulty difficulty;

    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private boolean isPremium;
    private String editorial;
    private String solutionTemplate;
    private List<Long> tagIds;
}
