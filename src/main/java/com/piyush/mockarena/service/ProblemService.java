package com.piyush.mockarena.service;

import com.piyush.mockarena.dto.ProblemResponse;
import com.piyush.mockarena.entity.Problem;
import com.piyush.mockarena.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProblemService {

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public Page<ProblemResponse> getAllProblems(Pageable pageable) {
        Page<Problem> problemPage = problemRepository.findByIsActiveTrue(pageable);

        List<ProblemResponse> responses = problemPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, problemPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProblemResponse> searchProblems(
            List<Problem.Difficulty> difficulties,
            String search,
            Pageable pageable) {

        Page<Problem> problemPage;

        // Handle different search scenarios
        if (search != null && difficulties != null && !difficulties.isEmpty()) {
            // Search with both query and difficulties
            problemPage = problemRepository.findByQueryAndDifficultiesAndIsActiveTrue(search, difficulties, pageable);
        } else if (search != null) {
            // Search with query only
            problemPage = problemRepository.findByTitleOrDescriptionContainingIgnoreCaseAndIsActiveTrue(search, pageable);
        } else if (difficulties != null && !difficulties.isEmpty()) {
            // Filter by difficulties only
            problemPage = problemRepository.findByDifficultyInAndIsActiveTrueOrderByCreatedAtDesc(difficulties, pageable);
        } else {
            // No filters, return all active problems
            problemPage = problemRepository.findByIsActiveTrue(pageable);
        }

        List<ProblemResponse> responses = problemPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, problemPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ProblemResponse> getPopularProblems(int limit) {
        List<Problem> problems = problemRepository.findTop10ByIsActiveTrueOrderByCreatedAtDesc();

        return problems.stream()
                .limit(limit)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProblemResponse getRandomProblem(Problem.Difficulty difficulty) {
        List<Problem> problems;

        if (difficulty != null) {
            problems = problemRepository.findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(difficulty);
        } else {
            problems = problemRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        }

        if (problems.isEmpty()) {
            return null;
        }

        Problem randomProblem = problems.get((int) (Math.random() * problems.size()));
        return convertToResponse(randomProblem);
    }

    @Transactional(readOnly = true)
    public List<ProblemResponse> getProblemsByDifficulty(Problem.Difficulty difficulty) {
        List<Problem> problems = problemRepository.findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(difficulty);

        return problems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProblemResponse getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));

        return convertToResponse(problem);
    }

    private ProblemResponse convertToResponse(Problem problem) {
        ProblemResponse response = new ProblemResponse();
        response.setId(problem.getId());
        response.setTitle(problem.getTitle());
        response.setDescription(problem.getDescription());
        response.setDifficulty(problem.getDifficulty());
        response.setCreatedBy(problem.getCreatedBy().getUsername());
        response.setCreatedAt(problem.getCreatedAt());
        response.setUpdatedAt(problem.getUpdatedAt());
        response.setActive(problem.isActive());
        return response;
    }
}
