// ProblemCreateRequest.java
package com.piyush.mockarena.dto;

import com.piyush.mockarena.entity.Problem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ProblemCreateRequest {
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

    private Integer timeLimitMs = 2000;
    private Integer memoryLimitMb = 256;
    private boolean isPremium = false;
    private String editorial;
    private String solutionTemplate;
    private List<Long> tagIds;
}
