package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class ProblemStatsResponse {
    private Long totalProblems;
    private Integer easyProblems;
    private Integer mediumProblems;
    private Integer hardProblems;
    private Long totalSubmissions;
    private Long acceptedSubmissions;
    private Double overallAcceptanceRate;
}
