package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class ContestStatsResponse {
    private Long contestId;
    private Integer totalParticipants;
    private Integer activeParticipants;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double averageScore;
    private String mostSolvedProblem;
    private String leastSolvedProblem;
    private Long timeRemaining; // in seconds
}
