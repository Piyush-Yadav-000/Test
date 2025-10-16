package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    // Judge0 submission token
    @Column(name = "judge0_token")
    private String judge0Token;

    // Execution results
    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    @Column(columnDefinition = "TEXT")
    private String compileOutput;

    @Column(name = "test_cases_passed")
    private Integer testCasesPassed = 0;

    @Column(name = "test_cases_total")
    private Integer testCasesTotal = 0;

    @Column(name = "score")
    private Double score = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        PENDING,           // Just submitted
        QUEUED,           // Sent to Judge0, waiting
        PROCESSING,       // Judge0 is executing
        COMPLETED,        // Execution finished
        ERROR,           // Compilation or runtime error
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        WRONG_ANSWER,
        ACCEPTED
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}