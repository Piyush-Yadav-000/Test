package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class LanguageStatsResponse {
    private String language;
    private Integer problemsSolved;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;
    private Double percentage;
}
