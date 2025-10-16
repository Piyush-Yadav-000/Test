// LeaderboardEntry.java
package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class LeaderboardEntry {
    private Integer rank;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private Integer totalScore;
    private Integer problemsSolved;
    private Integer penaltyTime;
    private Long finishTime;
    private Integer ratingChange;
    private Integer oldRating;
    private Integer newRating;
    private Boolean isLive;
}
