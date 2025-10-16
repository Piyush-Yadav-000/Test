// UserStatsResponse.java
package com.piyush.mockarena.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserStatsResponse {
    private Integer totalProblemsSolved;
    private Integer totalSubmissions;
    private Double acceptanceRate;
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer globalRank;
    private Integer contestRating;

    private DifficultyStatsResponse difficultyStats;
    private List<LanguageStatsResponse> languageStats;
    private List<ActivityResponse> activityData;
    private List<RecentSubmissionResponse> recentSubmissions;
    private List<MonthlyStatsResponse> monthlyProgress;
}
