package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentSubmissionResponse {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String language;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private LocalDateTime createdAt;
}
