package com.piyush.mockarena.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmissionStatsResponse {
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Integer wrongAnswerSubmissions;
    private Integer timeExceededSubmissions;
    private Integer runtimeErrorSubmissions;
    private Integer compilationErrorSubmissions;
    private Double acceptanceRate;
    private Integer averageRuntimeMs;
    private Integer averageMemoryKb;
    private List<LanguageStatsResponse> languageBreakdown;
    private List<DifficultyBreakdownResponse> difficultyBreakdown;
}
