package com.piyush.mockarena.controller;

import com.piyush.mockarena.dto.ProblemRequest;
import com.piyush.mockarena.dto.ProblemResponse;
import com.piyush.mockarena.entity.Problem;
import com.piyush.mockarena.entity.User;
import com.piyush.mockarena.entity.Language;
import com.piyush.mockarena.repository.ProblemRepository;
import com.piyush.mockarena.repository.UserRepository;
import com.piyush.mockarena.repository.LanguageRepository;
import com.piyush.mockarena.service.CodeTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem Management", description = "APIs for managing coding problems")
public class ProblemController {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final CodeTemplateService codeTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new problem")
    public ResponseEntity<Map<String, Object>> createProblem(@Valid @RequestBody ProblemRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            User creator = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Problem problem = new Problem();
            problem.setTitle(request.getTitle());
            problem.setDescription(request.getDescription());
            problem.setDifficulty(request.getDifficulty());
            problem.setCreatedBy(creator);

            // Set default template values
            problem.setFunctionName("solution");
            problem.setReturnType("int");
            problem.setUsesTemplate(true);

            Problem saved = problemRepository.save(problem);
            log.info("✅ Problem created by {}: {}", username, saved.getTitle());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Problem created successfully",
                    "problem", convertToResponse(saved)
            ));
        } catch (Exception e) {
            log.error("❌ Error creating problem", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error creating problem: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    @Operation(summary = "Get all problems with pagination and filtering")
    public ResponseEntity<Map<String, Object>> getAllProblems(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Difficulty filters") @RequestParam(required = false) List<String> difficulties) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Problem> problemPage;

            // Handle search and filtering
            if (search != null || (difficulties != null && !difficulties.isEmpty())) {
                problemPage = problemRepository.findByIsActiveTrue(pageable);

                // Filter results
                List<Problem> filteredProblems = problemPage.getContent().stream()
                        .filter(problem -> {
                            boolean matchesSearch = search == null ||
                                    problem.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                                    problem.getDescription().toLowerCase().contains(search.toLowerCase());

                            boolean matchesDifficulty = difficulties == null || difficulties.isEmpty() ||
                                    difficulties.contains(problem.getDifficulty().name());

                            return matchesSearch && matchesDifficulty;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("content", filteredProblems.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList()));
                response.put("totalElements", (long) filteredProblems.size());
                response.put("totalPages", 1);
                response.put("size", size);
                response.put("number", page);
                response.put("searchApplied", search != null);
                response.put("difficultiesApplied", difficulties != null && !difficulties.isEmpty());

                return ResponseEntity.ok(response);
            } else {
                problemPage = problemRepository.findByIsActiveTrue(pageable);

                List<ProblemResponse> responses = problemPage.getContent().stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("content", responses);
                response.put("totalElements", problemPage.getTotalElements());
                response.put("totalPages", problemPage.getTotalPages());
                response.put("size", problemPage.getSize());
                response.put("number", problemPage.getNumber());

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("❌ Error fetching problems", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error fetching problems: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search problems")
    public ResponseEntity<Map<String, Object>> searchProblems(
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Difficulty filters") @RequestParam(required = false) List<String> difficulties,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        try {
            return getAllProblems(page, size, "createdAt", "desc", query, difficulties);
        } catch (Exception e) {
            log.error("❌ Error searching problems", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error searching problems: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular problems")
    public ResponseEntity<Map<String, Object>> getPopularProblems(
            @Parameter(description = "Number of problems to return") @RequestParam(defaultValue = "10") int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by("totalSubmissions").descending());
            Page<Problem> problemPage = problemRepository.findByIsActiveTrue(pageable);

            List<ProblemResponse> responses = problemPage.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "content", responses,
                    "message", "Popular problems",
                    "totalCount", responses.size()
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching popular problems", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error fetching popular problems: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @GetMapping("/random")
    @Operation(summary = "Get a random problem")
    public ResponseEntity<Map<String, Object>> getRandomProblem(
            @Parameter(description = "Difficulty filter") @RequestParam(required = false) Problem.Difficulty difficulty) {
        try {
            List<Problem> problems;
            if (difficulty != null) {
                problems = problemRepository.findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(difficulty);
            } else {
                problems = problemRepository.findByIsActiveTrueOrderByCreatedAtDesc();
            }

            if (problems.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No problems found", "problem", null));
            }

            Problem randomProblem = problems.get((int) (Math.random() * problems.size()));

            return ResponseEntity.ok(Map.of(
                    "message", "Random problem selected",
                    "problem", convertToResponse(randomProblem),
                    "totalAvailable", problems.size()
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching random problem", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error fetching random problem: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @GetMapping("/difficulty/{difficulty}")
    @Operation(summary = "Get problems by difficulty")
    public ResponseEntity<Map<String, Object>> getProblemsByDifficulty(
            @Parameter(description = "Problem difficulty") @PathVariable Problem.Difficulty difficulty) {
        try {
            List<Problem> problems = problemRepository.findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(difficulty);

            List<ProblemResponse> responses = problems.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "content", responses,
                    "difficulty", difficulty.name(),
                    "totalCount", responses.size()
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching problems by difficulty", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error fetching problems: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    // ✅ Template endpoint with proper error handling and fallbacks
    @GetMapping("/{id}/template")
    @Operation(summary = "Get problem template for specified language")
    public ResponseEntity<Map<String, Object>> getProblemTemplate(
            @Parameter(description = "Problem ID") @PathVariable Long id,
            @Parameter(description = "Language ID") @RequestParam Integer languageId) {
        try {
            log.info("🔍 Getting template for problem {} and language {}", id, languageId);

            // Get problem
            Problem problem = problemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));

            log.info("✅ Problem found: {}", problem.getTitle());

            // Get language
            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new RuntimeException("Language not found with id: " + languageId));

            log.info("✅ Language found: {}", language.getName());

            // Generate template
            String template = codeTemplateService.getTemplate(id, languageId);
            String imports = codeTemplateService.getImports(language.getName().toLowerCase());

            Map<String, Object> response = new HashMap<>();
            response.put("template", template);
            response.put("language", language.getName().toLowerCase());
            response.put("imports", imports);
            response.put("problemTitle", problem.getTitle());
            response.put("functionName", problem.getFunctionName() != null ? problem.getFunctionName() : "solution");
            response.put("returnType", problem.getReturnType() != null ? problem.getReturnType() : "int");
            response.put("usesTemplate", problem.isUsesTemplate());
            response.put("success", true);

            log.info("✅ Template generated successfully for problem: {}", problem.getTitle());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting problem template: {}", e.getMessage(), e);

            // Return fallback template with error info
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("template", codeTemplateService.getDefaultTemplate(languageId));
            errorResponse.put("language", getLanguageNameById(languageId));
            errorResponse.put("imports", codeTemplateService.getImports(getLanguageNameById(languageId)));
            errorResponse.put("problemTitle", "Problem " + id);
            errorResponse.put("functionName", "solution");
            errorResponse.put("returnType", "int");
            errorResponse.put("usesTemplate", true);
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            log.warn("⚠️ Using fallback template for problem {} language {}", id, languageId);
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PostMapping("/generate-all-templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate templates for all problems (Admin only)")
    public ResponseEntity<Map<String, Object>> generateAllTemplates() {
        try {
            log.info("🚀 Admin requested template generation for all problems");

            // Get statistics
            List<Problem> problems = problemRepository.findByIsActiveTrueOrderByCreatedAtDesc();
            List<Language> languages = languageRepository.findByActiveTrueOrderBySortOrder();

            int totalCombinations = problems.size() * languages.size();
            int successCount = 0;
            int errorCount = 0;

            // Generate templates for each problem-language combination
            for (Problem problem : problems) {
                for (Language language : languages) {
                    try {
                        // Generate template for this combination
                        String template = codeTemplateService.getTemplate(problem.getId(), language.getId());
                        if (template != null && !template.trim().isEmpty()) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        errorCount++;
                        log.warn("Failed to generate template for problem {} in {}: {}",
                                problem.getTitle(), language.getDisplayName(), e.getMessage());
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Template generation completed");
            response.put("totalProblems", problems.size());
            response.put("totalLanguages", languages.size());
            response.put("totalCombinations", totalCombinations);
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error in template generation: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to generate templates: " + e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            ));
        }
    }


    // ✅ Get single problem endpoint (MUST be after specific endpoints)
    @GetMapping("/{id}")
    @Operation(summary = "Get problem by ID")
    public ResponseEntity<Object> getProblem(
            @Parameter(description = "Problem ID") @PathVariable Long id) {
        try {
            log.info("🔍 Fetching problem with ID: {}", id);

            Problem problem = problemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));

            log.info("✅ Problem found: {} ({})", problem.getTitle(), problem.getDifficulty());

            // ✅ Return complete response structure that frontend expects
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", problem.getId());
            response.put("title", problem.getTitle());
            response.put("description", problem.getDescription());
            response.put("difficulty", problem.getDifficulty().name());
            response.put("acceptanceRate", problem.getAcceptanceRate());
            response.put("totalSubmissions", problem.getTotalSubmissions());
            response.put("tags", problem.getTags());
            response.put("sampleInput", problem.getSampleInput());
            response.put("sampleOutput", problem.getSampleOutput());
            response.put("explanation", problem.getExplanation());
            response.put("constraints", problem.getConstraints());
            response.put("inputFormat", problem.getInputFormat());
            response.put("outputFormat", problem.getOutputFormat());
            response.put("timeLimitMs", problem.getTimeLimitMs());
            response.put("memoryLimitMb", problem.getMemoryLimitMb());
            response.put("createdBy", problem.getCreatedBy().getUsername());
            response.put("createdAt", problem.getCreatedAt());
            response.put("updatedAt", problem.getUpdatedAt());
            response.put("isActive", problem.isActive());

            // Template-related fields for frontend
            response.put("functionName", problem.getFunctionName() != null ? problem.getFunctionName() : "solution");
            response.put("returnType", problem.getReturnType() != null ? problem.getReturnType() : "int");
            response.put("usesTemplate", problem.isUsesTemplate());

            // Parameters if available
            if (problem.getParameters() != null && !problem.getParameters().trim().isEmpty()) {
                response.put("parameters", problem.getParameters());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching problem with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Problem not found: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update problem")
    public ResponseEntity<Map<String, Object>> updateProblem(
            @Parameter(description = "Problem ID") @PathVariable Long id,
            @Valid @RequestBody ProblemRequest request) {
        try {
            Problem problem = problemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            problem.setTitle(request.getTitle());
            problem.setDescription(request.getDescription());
            problem.setDifficulty(request.getDifficulty());

            Problem updated = problemRepository.save(problem);
            log.info("✅ Problem updated: {}", updated.getTitle());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Problem updated successfully",
                    "problem", convertToResponse(updated)
            ));
        } catch (Exception e) {
            log.error("❌ Error updating problem", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error updating problem: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete problem (soft delete)")
    public ResponseEntity<Map<String, Object>> deleteProblem(
            @Parameter(description = "Problem ID") @PathVariable Long id) {
        try {
            Problem problem = problemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            problem.setActive(false);
            problemRepository.save(problem);
            log.info("✅ Problem deactivated: {}", problem.getTitle());

            return ResponseEntity.ok(Map.of(
                    "message", "Problem deleted successfully",
                    "success", true
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting problem", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error deleting problem: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    // ✅ HELPER METHODS
    private ProblemResponse convertToResponse(Problem problem) {
        ProblemResponse response = new ProblemResponse();
        response.setId(problem.getId());
        response.setTitle(problem.getTitle());
        response.setDescription(problem.getDescription());
        response.setDifficulty(problem.getDifficulty());
        response.setAcceptanceRate(problem.getAcceptanceRate());
        response.setTotalSubmissions(problem.getTotalSubmissions());
        response.setSampleInput(problem.getSampleInput());
        response.setSampleOutput(problem.getSampleOutput());
        response.setExplanation(problem.getExplanation());
        response.setConstraints(problem.getConstraints());
        response.setInputFormat(problem.getInputFormat());
        response.setOutputFormat(problem.getOutputFormat());
        response.setTimeLimitMs(problem.getTimeLimitMs());
        response.setMemoryLimitMb(problem.getMemoryLimitMb());
        response.setCreatedAt(problem.getCreatedAt());
        response.setUpdatedAt(problem.getUpdatedAt());
        response.setActive(problem.isActive());

        if (problem.getCreatedBy() != null) {
            response.setCreatedBy(problem.getCreatedBy().getUsername());
        }

        return response;
    }

    private String getLanguageNameById(Integer languageId) {
        return switch (languageId) {
            case 62 -> "java";
            case 71 -> "python";
            case 54 -> "cpp";
            case 63 -> "javascript";
            case 50 -> "c";
            case 60 -> "go";
            case 51 -> "csharp";
            case 78 -> "kotlin";
            default -> "java";
        };
    }
}
