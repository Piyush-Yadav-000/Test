// UserProfileResponse.java
package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private String githubUsername;
    private String linkedinUrl;
    private String currentCompany;
    private String currentPosition;
    private String location;
    private String websiteUrl;

    // Statistics
    private Integer totalProblemsSolved;
    private Integer easyProblemsSolved;
    private Integer mediumProblemsSolved;
    private Integer hardProblemsSolved;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;

    // Contest stats
    private Integer contestRating;
    private Integer maxContestRating;
    private Integer contestsParticipated;
    private Integer globalRank;
    private Integer countryRank;

    // Activity
    private String preferredLanguage;
    private Integer streakCount;
    private Integer maxStreakCount;
    private LocalDateTime lastActiveDate;
    private LocalDateTime joinDate;

    private boolean isPublicProfile;
}
