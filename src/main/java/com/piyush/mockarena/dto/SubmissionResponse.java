package com.piyush.mockarena.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionResponse {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String language;
    private String status;
    private String sourceCode;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private Double score;
    private String stdout;
    private String stderr;
    private String compileOutput;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ✅ NEW FIELD - This was missing!
    private String message;
}
