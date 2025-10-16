package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class ProblemSummaryResponse {
    private Long id;
    private String title;
    private String difficulty;
    private Double acceptanceRate;
}
