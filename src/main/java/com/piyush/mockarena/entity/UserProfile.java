package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(name = "current_position")
    private String currentPosition;

    @Column(name = "location")
    private String location;

    @Column(name = "website_url")
    private String websiteUrl;

    // Statistics
    @Column(name = "total_problems_solved")
    private Integer totalProblemsSolved = 0;

    @Column(name = "easy_problems_solved")
    private Integer easyProblemsSolved = 0;

    @Column(name = "medium_problems_solved")
    private Integer mediumProblemsSolved = 0;

    @Column(name = "hard_problems_solved")
    private Integer hardProblemsSolved = 0;

    @Column(name = "total_submissions")
    private Integer totalSubmissions = 0;

    @Column(name = "accepted_submissions")
    private Integer acceptedSubmissions = 0;

    @Column(name = "contest_rating")
    private Integer contestRating = 1500;

    @Column(name = "max_contest_rating")
    private Integer maxContestRating = 1500;

    @Column(name = "contests_participated")
    private Integer contestsParticipated = 0;

    @Column(name = "global_rank")
    private Integer globalRank;

    @Column(name = "country_rank")
    private Integer countryRank;

    @Column(name = "preferred_language")
    private String preferredLanguage = "Java";

    @Column(name = "streak_count")
    private Integer streakCount = 0;

    @Column(name = "max_streak_count")
    private Integer maxStreakCount = 0;

    @Column(name = "last_active_date")
    private LocalDateTime lastActiveDate;

    @Column(name = "is_public_profile")
    private boolean isPublicProfile = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public Double getAcceptanceRate() {
        if (totalSubmissions == 0) return 0.0;
        return (acceptedSubmissions.doubleValue() / totalSubmissions.doubleValue()) * 100;
    }

    public void incrementProblemsSolved(Problem.Difficulty difficulty) {
        totalProblemsSolved++;
        switch (difficulty) {
            case EASY -> easyProblemsSolved++;
            case MEDIUM -> mediumProblemsSolved++;
            case HARD -> hardProblemsSolved++;
        }
    }

    public void updateStreaks() {
        LocalDateTime today = LocalDateTime.now();
        if (lastActiveDate != null && lastActiveDate.toLocalDate().equals(today.toLocalDate().minusDays(1))) {
            streakCount++;
            if (streakCount > maxStreakCount) {
                maxStreakCount = streakCount;
            }
        } else if (lastActiveDate == null || !lastActiveDate.toLocalDate().equals(today.toLocalDate())) {
            streakCount = 1;
        }
        lastActiveDate = today;
    }
}
