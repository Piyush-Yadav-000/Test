package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // THE MISSING METHOD - ADD THIS LINE:
    long countByDifficulty(Problem.Difficulty difficulty);

    // Your existing methods:
    Page<Problem> findByIsActiveTrue(Pageable pageable);
    List<Problem> findByIsActiveTrueOrderByCreatedAtDesc();
    List<Problem> findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(Problem.Difficulty difficulty);
    long countByDifficultyAndIsActiveTrue(Problem.Difficulty difficulty);

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Problem> findByTitleContainingIgnoreCaseAndIsActiveTrue(@Param("title") String title);

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Problem> findByTitleOrDescriptionContainingIgnoreCaseAndIsActiveTrue(@Param("query") String query);

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Problem> findByTitleOrDescriptionContainingIgnoreCaseAndIsActiveTrue(@Param("query") String query, Pageable pageable);

    List<Problem> findByCreatedBy_UsernameAndIsActiveTrueOrderByCreatedAtDesc(String username);
    List<Problem> findTop10ByIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND p.difficulty IN :difficulties ORDER BY p.createdAt DESC")
    List<Problem> findByDifficultyInAndIsActiveTrueOrderByCreatedAtDesc(@Param("difficulties") List<Problem.Difficulty> difficulties);

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND p.difficulty IN :difficulties ORDER BY p.createdAt DESC")
    Page<Problem> findByDifficultyInAndIsActiveTrueOrderByCreatedAtDesc(@Param("difficulties") List<Problem.Difficulty> difficulties, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "p.difficulty IN :difficulties ORDER BY p.createdAt DESC")
    List<Problem> findByQueryAndDifficultiesAndIsActiveTrue(
            @Param("query") String query,
            @Param("difficulties") List<Problem.Difficulty> difficulties);

    @Query("SELECT p FROM Problem p WHERE p.isActive = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "p.difficulty IN :difficulties ORDER BY p.createdAt DESC")
    Page<Problem> findByQueryAndDifficultiesAndIsActiveTrue(
            @Param("query") String query,
            @Param("difficulties") List<Problem.Difficulty> difficulties,
            Pageable pageable);

    long countByIsActiveTrue();
}
