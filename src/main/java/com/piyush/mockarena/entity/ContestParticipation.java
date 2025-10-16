package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "contest_participations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contest_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContestParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registration_time", nullable = false)
    private LocalDateTime registrationTime = LocalDateTime.now();

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_score")
    private Integer totalScore = 0;

    @Column(name = "problems_solved")
    private Integer problemsSolved = 0;

    @Column(name = "penalty_time")
    private Integer penaltyTime = 0; // in minutes

    @Column(name = "final_rank")
    private Integer finalRank;

    @Column(name = "rating_change")
    private Integer ratingChange = 0;

    @Column(name = "old_rating")
    private Integer oldRating;

    @Column(name = "new_rating")
    private Integer newRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status = ParticipationStatus.REGISTERED;

    public enum ParticipationStatus {
        REGISTERED("Registered"),
        ACTIVE("Active"),
        COMPLETED("Completed"),
        DISQUALIFIED("Disqualified");

        private final String displayName;

        ParticipationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Helper methods
    public void calculateScore(int problemScore, int timePenalty) {
        this.totalScore += problemScore;
        this.penaltyTime += timePenalty;
        this.problemsSolved++;
    }

    public Long getContestDurationMinutes() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return null;
    }
}
