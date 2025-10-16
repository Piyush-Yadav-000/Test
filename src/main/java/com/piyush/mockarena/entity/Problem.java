package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Description is required")
    private String description;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "constraints", columnDefinition = "TEXT")
    private String constraints;

    @Column(name = "sample_input", columnDefinition = "TEXT")
    private String sampleInput;

    @Column(name = "sample_output", columnDefinition = "TEXT")
    private String sampleOutput;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "acceptance_rate")
    private Double acceptanceRate = 0.0;

    @Column(name = "total_submissions")
    private Integer totalSubmissions = 0;

    @Column(name = "accepted_submissions")
    private Integer acceptedSubmissions = 0;

    @Column(name = "likes_count")
    private Integer likesCount = 0;

    @Column(name = "dislikes_count")
    private Integer dislikesCount = 0;

    @Column(name = "time_limit_ms")
    private Integer timeLimitMs = 2000; // 2 seconds default

    @Column(name = "memory_limit_mb")
    private Integer memoryLimitMb = 256; // 256 MB default

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "problem_tags_mapping",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ProblemTag> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_premium")
    private boolean isPremium = false;

    @Column(name = "company_tags")
    private String companyTags; // JSON string for company associations

    @Column(name = "editorial", columnDefinition = "TEXT")
    private String editorial;

    @Column(name = "solution_template", columnDefinition = "TEXT")
    private String solutionTemplate;

    @Column(columnDefinition = "LONGTEXT")
    private String codeTemplate; // JSON with language templates

    @Column(columnDefinition = "LONGTEXT")
    private String starterCode; // JSON with starter code for each language

    @Column(columnDefinition = "LONGTEXT")
    private String driverCode; // JSON with driver code for each language

    @Column(columnDefinition = "LONGTEXT")
    private String visibleTestCases; // JSON array of visible test cases

    @Column(columnDefinition = "LONGTEXT")
    private String hiddenTestCases; // JSON array of hidden test cases

    // ✅ TEMPLATE-RELATED FIELDS (Your current fields are perfect!)
    @Column(name = "function_name")
    private String functionName = "solution"; // Main function name to implement

    @Column(name = "return_type")
    private String returnType = "int"; // Return type of function

    @Column(name = "function_signature", columnDefinition = "TEXT")
    private String functionSignature; // Generic function signature

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters; // JSON array of parameters

    @Column(name = "uses_template")
    private boolean usesTemplate = true; // Whether to use LeetCode-style templates

    @Column(name = "method_signature_java", columnDefinition = "TEXT")
    private String methodSignatureJava;

    @Column(name = "method_signature_python", columnDefinition = "TEXT")
    private String methodSignaturePython;

    @Column(name = "method_signature_cpp", columnDefinition = "TEXT")
    private String methodSignatureCpp;

    @Column(name = "method_signature_javascript", columnDefinition = "TEXT")
    private String methodSignatureJavascript;

    @Column(name = "method_signature_csharp", columnDefinition = "TEXT")
    private String methodSignatureCsharp;

    @Column(name = "hints", columnDefinition = "LONGTEXT")
    private String hints; // JSON array of hints

    @Column(name = "follow_up", columnDefinition = "TEXT")
    private String followUp;

    // ✅ Add these fields to your Problem entity
    @Column(name = "java_signature", columnDefinition = "TEXT")
    private String javaSignature;

    @Column(name = "python_signature", columnDefinition = "TEXT")
    private String pythonSignature;

    @Column(name = "cpp_signature", columnDefinition = "TEXT")
    private String cppSignature;

    @Column(name = "javascript_signature", columnDefinition = "TEXT")
    private String javascriptSignature;

    @Column(name = "csharp_signature", columnDefinition = "TEXT")
    private String csharpSignature;




    public enum Difficulty {
        EASY("Easy", 1),
        MEDIUM("Medium", 2),
        HARD("Hard", 3);

        private final String displayName;
        private final int level;

        Difficulty(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }
    }

    // ✅ TEMPLATE-RELATED METHODS (Your current methods are perfect!)
    public boolean isTemplateEnabled() {
        return usesTemplate;
    }

    public String getMethodSignatureForLanguage(String languageKey) {
        return switch (languageKey.toLowerCase()) {
            case "java" -> methodSignatureJava;
            case "python" -> methodSignaturePython;
            case "cpp" -> methodSignatureCpp;
            case "javascript" -> methodSignatureJavascript;
            case "csharp" -> methodSignatureCsharp;
            default -> functionSignature;
        };
    }

    public String getEffectiveFunctionSignature() {
        if (functionSignature != null && !functionSignature.trim().isEmpty()) {
            return functionSignature;
        }

        // Generate default signature
        String funcName = functionName != null ? functionName : "solution";
        String retType = returnType != null ? returnType : "int";
        return String.format("public %s %s()", retType, funcName);
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAcceptanceRate();
    }

    @PrePersist
    public void prePersist() {
        if (slug == null && title != null) {
            slug = generateSlug(title);
        }
        // Set defaults for template fields
        if (functionName == null) functionName = "solution";
        if (returnType == null) returnType = "int";
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private void calculateAcceptanceRate() {
        if (totalSubmissions > 0) {
            acceptanceRate = (acceptedSubmissions.doubleValue() / totalSubmissions.doubleValue()) * 100;
        }
    }

    // Helper methods
    public void incrementSubmissions() {
        totalSubmissions++;
        calculateAcceptanceRate();
    }

    public void incrementAcceptedSubmissions() {
        acceptedSubmissions++;
        calculateAcceptanceRate();
    }

    public boolean isEasy() {
        return difficulty == Difficulty.EASY;
    }

    public boolean isMedium() {
        return difficulty == Difficulty.MEDIUM;
    }

    public boolean isHard() {
        return difficulty == Difficulty.HARD;
    }

    public String getDifficultyColor() {
        return switch (difficulty) {
            case EASY -> "#00b8a3";
            case MEDIUM -> "#ffc01e";
            case HARD -> "#ff375f";
        };
    }

    // ✅ ADDED: ToString for debugging
    @Override
    public String toString() {
        return "Problem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", difficulty=" + difficulty +
                ", functionName='" + functionName + '\'' +
                ", returnType='" + returnType + '\'' +
                ", usesTemplate=" + usesTemplate +
                ", isActive=" + isActive +
                '}';
    }
}
