package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class DifficultyBreakdownResponse {
    private String difficulty;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;
}
