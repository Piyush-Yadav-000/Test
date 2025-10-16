package com.piyush.mockarena.dto;

import com.piyush.mockarena.entity.Contest;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContestResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Contest.ContestType type;
    private Contest.ContestStatus status;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private LocalDateTime registrationEndTime;
    private Boolean isRegistered;
    private Boolean canRegister;
    private Boolean isRated;  // Note: Boolean isRated, not rated
    private String prizePool;
    private String createdBy;
    private LocalDateTime createdAt;
    private Long timeRemaining; // in seconds
    private Integer problemCount;
}
