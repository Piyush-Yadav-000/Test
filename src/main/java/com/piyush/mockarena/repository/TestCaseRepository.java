package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.TestCase;
import com.piyush.mockarena.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // ✅ EXISTING METHODS (KEEP AS-IS)
    List<TestCase> findByProblemAndIsActiveTrueOrderBySortOrder(Problem problem);

    List<TestCase> findByProblemAndTypeAndIsActiveTrueOrderBySortOrder(Problem problem, TestCase.Type type);

    long countByProblemAndIsActiveTrue(Problem problem);

    long countByProblemAndTypeAndIsActiveTrue(Problem problem, TestCase.Type type);

    // ✅ NEW METHODS FOR LEETCODE-STYLE FUNCTIONALITY

    // Get visible test cases (PUBLIC type) by problem ID
    @Query("SELECT tc FROM TestCase tc WHERE tc.problem.id = :problemId AND tc.type = 'PUBLIC' AND tc.isActive = true ORDER BY tc.sortOrder")
    List<TestCase> findVisibleTestCasesByProblemId(@Param("problemId") Long problemId);

    // Get hidden test cases (HIDDEN type) by problem ID
    @Query("SELECT tc FROM TestCase tc WHERE tc.problem.id = :problemId AND tc.type = 'HIDDEN' AND tc.isActive = true ORDER BY tc.sortOrder")
    List<TestCase> findHiddenTestCasesByProblemId(@Param("problemId") Long problemId);

    // Get all test cases by problem ID (both PUBLIC and HIDDEN)
    @Query("SELECT tc FROM TestCase tc WHERE tc.problem.id = :problemId AND tc.isActive = true ORDER BY tc.sortOrder")
    List<TestCase> findAllTestCasesByProblemId(@Param("problemId") Long problemId);

    // Count visible test cases
    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.problem.id = :problemId AND tc.type = 'PUBLIC' AND tc.isActive = true")
    Integer countVisibleTestCasesByProblemId(@Param("problemId") Long problemId);

    // Count all test cases (for submission evaluation)
    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.problem.id = :problemId AND tc.isActive = true")
    Integer countAllTestCasesByProblemId(@Param("problemId") Long problemId);

    // ✅ REMOVED THE PROBLEMATIC METHOD - Use findVisibleTestCasesByProblemId instead
    // We don't need the limit method for now

    // For backward compatibility
    List<TestCase> findByProblemIdAndIsActiveTrueOrderBySortOrder(Long problemId);

    List<TestCase> findByProblemIdAndTypeAndIsActiveTrueOrderBySortOrder(Long problemId, TestCase.Type type);


    // Add this method to your TestCaseRepository
    List<TestCase> findByProblemOrderBySortOrderAsc(Problem problem);

    // Alternative if you prefer by ID:
    List<TestCase> findByProblemIdOrderBySortOrderAsc(Long problemId);


}
