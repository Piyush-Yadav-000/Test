package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestType type = ContestType.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestStatus status = ContestStatus.UPCOMING;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "registration_end_time")
    private LocalDateTime registrationEndTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "contest_problems",
            joinColumns = @JoinColumn(name = "contest_id"),
            inverseJoinColumns = @JoinColumn(name = "problem_id")
    )
    private List<Problem> problems = new ArrayList<>();

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContestParticipation> participants = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_rated")
    private boolean isRated = true;

    @Column(name = "prize_pool")
    private String prizePool;

    public enum ContestType {
        PUBLIC("Public Contest"),
        PRIVATE("Private Contest"),
        COMPANY_SPECIFIC("Company Specific"),
        WEEKLY("Weekly Contest"),
        BIWEEKLY("Biweekly Contest");

        private final String displayName;

        ContestType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ContestStatus {
        UPCOMING("Upcoming"),
        REGISTRATION_OPEN("Registration Open"),
        LIVE("Live"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        private final String displayName;

        ContestStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isRegistrationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return status == ContestStatus.REGISTRATION_OPEN &&
                (registrationEndTime == null || now.isBefore(registrationEndTime));
    }

    public boolean isLive() {
        LocalDateTime now = LocalDateTime.now();
        return status == ContestStatus.LIVE &&
                now.isAfter(startTime) && now.isBefore(endTime);
    }

    public boolean isCompleted() {
        return status == ContestStatus.COMPLETED || LocalDateTime.now().isAfter(endTime);
    }

    public Integer getCurrentParticipants() {
        return participants != null ? participants.size() : 0;
    }

    public boolean canRegister() {
        return isRegistrationOpen() &&
                (maxParticipants == null || getCurrentParticipants() < maxParticipants);
    }
}
