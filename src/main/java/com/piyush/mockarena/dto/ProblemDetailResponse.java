// ProblemDetailResponse.java
package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProblemDetailResponse {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private String sampleInput;
    private String sampleOutput;
    private String explanation;
    private String difficulty;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Double acceptanceRate;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Integer likesCount;
    private Integer dislikesCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean isPremium;
    private String editorial;
    private String solutionTemplate;
    private List<ProblemTagResponse> tagDetails;
    private Boolean isSolved;
}
