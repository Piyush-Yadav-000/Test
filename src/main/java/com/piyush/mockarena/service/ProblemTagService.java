package com.piyush.mockarena.service;

import com.piyush.mockarena.dto.*;
import com.piyush.mockarena.entity.ProblemTag;
import com.piyush.mockarena.repository.ProblemTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProblemTagService {

    private final ProblemTagRepository problemTagRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "tags", key = "'all'")
    public List<ProblemTagResponse> getAllActiveTags() {
        List<ProblemTag> tags = problemTagRepository.findByIsActiveTrue();
        return tags.stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProblemTagResponse> getTagsByCategory(ProblemTag.TagCategory category) {
        List<ProblemTag> tags = problemTagRepository.findByCategoryAndIsActiveTrue(category);
        return tags.stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProblemTagResponse> searchTags(String query) {
        List<ProblemTag> tags = problemTagRepository.searchTags(query);
        return tags.stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TagWithCountResponse> getTagsWithProblemCount() {
        List<Object[]> results = problemTagRepository.findTagsWithProblemCount();
        return results.stream()
                .map(row -> {
                    ProblemTag tag = (ProblemTag) row[0];
                    Long count = ((Number) row[1]).longValue();

                    TagWithCountResponse response = new TagWithCountResponse();
                    response.setId(tag.getId());
                    response.setName(tag.getName());
                    response.setDisplayName(tag.getDisplayName());
                    response.setCategory(tag.getCategory().name());
                    response.setColorCode(tag.getColorCode());
                    response.setProblemCount(count.intValue());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "tags", allEntries = true)
    public ProblemTagResponse createTag(ProblemTagRequest request) {
        ProblemTag tag = new ProblemTag();
        tag.setName(request.getName());
        tag.setDisplayName(request.getDisplayName());
        tag.setDescription(request.getDescription());
        tag.setCategory(request.getCategory());
        tag.setColorCode(request.getColorCode());
        tag.setSortOrder(request.getSortOrder());

        ProblemTag saved = problemTagRepository.save(tag);
        log.info("Tag created: {}", saved.getName());

        return mapToTagResponse(saved);
    }

    @CacheEvict(value = "tags", allEntries = true)
    public ProblemTagResponse updateTag(Long id, ProblemTagRequest request) {
        ProblemTag tag = problemTagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        tag.setDisplayName(request.getDisplayName());
        tag.setDescription(request.getDescription());
        tag.setCategory(request.getCategory());
        tag.setColorCode(request.getColorCode());
        tag.setSortOrder(request.getSortOrder());

        ProblemTag saved = problemTagRepository.save(tag);
        log.info("Tag updated: {}", saved.getName());

        return mapToTagResponse(saved);
    }

    @CacheEvict(value = "tags", allEntries = true)
    public void deleteTag(Long id) {
        ProblemTag tag = problemTagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        tag.setActive(false);
        problemTagRepository.save(tag);
        log.info("Tag deleted: {}", tag.getName());
    }

    private ProblemTagResponse mapToTagResponse(ProblemTag tag) {
        ProblemTagResponse response = new ProblemTagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setDisplayName(tag.getDisplayName());
        response.setDescription(tag.getDescription());
        response.setCategory(tag.getCategory().name());
        response.setColorCode(tag.getColorCode());
        response.setSortOrder(tag.getSortOrder());
        response.setActive(tag.isActive());
        return response;
    }
}
