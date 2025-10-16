package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParticipationResponse {
    private Long id;
    private Long contestId;
    private String contestTitle;
    private String username;
    private LocalDateTime registrationTime;
    private String status;
    private Integer totalScore;
    private Integer problemsSolved;
    private Integer penaltyTime;
    private Integer finalRank;
    private Integer ratingChange;
}
