// ContestRequest.java
package com.piyush.mockarena.dto;

import com.piyush.mockarena.entity.Contest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    @NotNull(message = "Contest type is required")
    private Contest.ContestType type;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private Integer maxParticipants;

    private LocalDateTime registrationEndTime;

    @NotNull(message = "Problems are required")
    private List<Long> problemIds;

    private boolean isRated = true;

    private String prizePool;
}
