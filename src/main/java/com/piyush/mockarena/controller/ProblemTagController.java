package com.piyush.mockarena.controller;

import com.piyush.mockarena.dto.*;
import com.piyush.mockarena.entity.ProblemTag;
import com.piyush.mockarena.service.ProblemTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem Tags", description = "Problem categorization and tagging system")
public class ProblemTagController {

    private final ProblemTagService problemTagService;

    @Operation(summary = "Get all tags", description = "Retrieve all active problem tags")
    @GetMapping
    public ResponseEntity<List<ProblemTagResponse>> getAllTags() {
        List<ProblemTagResponse> tags = problemTagService.getAllActiveTags();
        return ResponseEntity.ok(tags);
    }

    @Operation(summary = "Get tags by category", description = "Retrieve tags filtered by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProblemTagResponse>> getTagsByCategory(
            @Parameter(description = "Tag category") @PathVariable ProblemTag.TagCategory category) {
        List<ProblemTagResponse> tags = problemTagService.getTagsByCategory(category);
        return ResponseEntity.ok(tags);
    }

    @Operation(summary = "Search tags", description = "Search tags by name or display name")
    @GetMapping("/search")
    public ResponseEntity<List<ProblemTagResponse>> searchTags(
            @Parameter(description = "Search query") @RequestParam String query) {
        List<ProblemTagResponse> tags = problemTagService.searchTags(query);
        return ResponseEntity.ok(tags);
    }

    @Operation(summary = "Get tag with problem count", description = "Get tags with their associated problem counts")
    @GetMapping("/with-counts")
    public ResponseEntity<List<TagWithCountResponse>> getTagsWithCounts() {
        List<TagWithCountResponse> tags = problemTagService.getTagsWithProblemCount();
        return ResponseEntity.ok(tags);
    }

    @Operation(summary = "Create tag", description = "Create a new problem tag (Admin only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProblemTagResponse> createTag(
            @Valid @RequestBody ProblemTagRequest request,
            Authentication authentication) {

        ProblemTagResponse tag = problemTagService.createTag(request);
        log.info("Tag created: {} by admin: {}", tag.getName(), authentication.getName());

        return ResponseEntity.ok(tag);
    }

    @Operation(summary = "Update tag", description = "Update an existing tag (Admin only)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProblemTagResponse> updateTag(
            @Parameter(description = "Tag ID") @PathVariable Long id,
            @Valid @RequestBody ProblemTagRequest request,
            Authentication authentication) {

        ProblemTagResponse tag = problemTagService.updateTag(id, request);
        log.info("Tag updated: {} by admin: {}", tag.getName(), authentication.getName());

        return ResponseEntity.ok(tag);
    }

    @Operation(summary = "Delete tag", description = "Soft delete a tag (Admin only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteTag(
            @Parameter(description = "Tag ID") @PathVariable Long id,
            Authentication authentication) {

        problemTagService.deleteTag(id);
        log.info("Tag deleted: {} by admin: {}", id, authentication.getName());

        return ResponseEntity.ok(new ApiResponse("Tag deleted successfully", true));
    }
}
