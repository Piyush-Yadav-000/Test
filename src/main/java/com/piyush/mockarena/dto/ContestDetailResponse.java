// ContestDetailResponse.java
package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestDetailResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private List<ProblemSummaryResponse> problems;
    private Boolean isRegistered;
    private Boolean canRegister;
    private Boolean isRated;
    private String prizePool;
    private String createdBy;
    private Long timeRemaining;
    private ContestStatsResponse stats;
}
