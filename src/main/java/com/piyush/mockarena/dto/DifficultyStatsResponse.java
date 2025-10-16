package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class DifficultyStatsResponse {
    private Integer easy;
    private Integer medium;
    private Integer hard;
    private Integer totalEasy;
    private Integer totalMedium;
    private Integer totalHard;
    private Double easyPercentage;
    private Double mediumPercentage;
    private Double hardPercentage;
}
