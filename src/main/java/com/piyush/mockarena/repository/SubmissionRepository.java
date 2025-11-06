package com.piyush.mockarena.repository;

import ch.qos.logback.core.status.Status;
import com.piyush.mockarena.entity.Submission;
import com.piyush.mockarena.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // ✅ Your existing methods (keep all of these)
    Page<Submission> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    List<Submission> findByUserUsernameAndProblemIdOrderByCreatedAtDesc(String username, Long problemId);

    Page<Submission> findByProblemIdOrderByCreatedAtDesc(Long problemId, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.user.username = :username AND s.status = :status")
    Page<Submission> findByUserUsernameAndStatus(@Param("username") String username,
                                                 @Param("status") Submission.Status status,
                                                 Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.problem.id = :problemId AND s.status = 'ACCEPTED'")
    List<Submission> findAcceptedSubmissionsByProblem(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.username = :username AND s.status = 'ACCEPTED'")
    Integer countAcceptedSubmissionsByUser(@Param("username") String username);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.username = :username")
    Integer countSubmissionsByUser(@Param("username") String username);

    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.username = :username AND s.status = 'ACCEPTED'")
    Integer countUniqueSolvedProblemsByUser(@Param("username") String username);

    @Query("SELECT s.problem.difficulty, COUNT(DISTINCT s.problem.id) FROM Submission s " +
            "WHERE s.user.username = :username AND s.status = 'ACCEPTED' " +
            "GROUP BY s.problem.difficulty")
    List<Object[]> countSolvedProblemsByDifficulty(@Param("username") String username);

    @Query("SELECT s.language.name, COUNT(s) FROM Submission s WHERE s.user.username = :username " +
            "GROUP BY s.language.name ORDER BY COUNT(s) DESC")
    List<Object[]> getLanguageUsageByUser(@Param("username") String username);

    @Query("SELECT DATE(s.createdAt), COUNT(s) FROM Submission s WHERE s.user.username = :username " +
            "AND s.createdAt >= :startDate GROUP BY DATE(s.createdAt)")
    List<Object[]> getSubmissionActivityByUser(@Param("username") String username,
                                               @Param("startDate") LocalDateTime startDate);

    @Query("SELECT s FROM Submission s WHERE s.createdAt >= :startTime ORDER BY s.createdAt DESC")
    List<Submission> findRecentSubmissions(@Param("startTime") LocalDateTime startTime, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.problem.id = :problemId AND s.user.username = :username " +
            "AND s.status = 'ACCEPTED' ORDER BY s.createdAt ASC")
    Optional<Submission> findFirstAcceptedSubmission(@Param("problemId") Long problemId,
                                                     @Param("username") String username);

    @Query("SELECT AVG(s.runtimeMs) FROM Submission s WHERE s.problem.id = :problemId AND s.status = 'ACCEPTED'")
    Double getAverageRuntimeForProblem(@Param("problemId") Long problemId);

    @Query("SELECT s FROM Submission s WHERE s.problem.id = :problemId AND s.status = 'ACCEPTED' " +
            "ORDER BY s.runtimeMs ASC")
    List<Submission> findFastestSubmissionsForProblem(@Param("problemId") Long problemId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem.id = :problemId")
    Integer countSubmissionsByProblem(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem.id = :problemId AND s.status = 'ACCEPTED'")
    Integer countAcceptedSubmissionsByProblem(@Param("problemId") Long problemId);

    boolean existsByUserUsernameAndProblemIdAndStatus(String username, Long problemId, Submission.Status status);

    @Query("SELECT DISTINCT s.problem FROM Submission s WHERE s.user.username = :username AND s.status = 'ACCEPTED'")
    List<Problem> findSolvedProblemsByUser(@Param("username") String username);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.username = :username")
    Integer countByUserUsername(@Param("username") String username);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.username = :username AND s.status = :status")
    Integer countByUserUsernameAndStatus(@Param("username") String username, @Param("status") Submission.Status status);

    // ✅ ADD THESE MISSING METHODS FOR CodeExecutionService

    /**
     * Count total submissions for a specific problem
     */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem = :problem")
    long countByProblem(@Param("problem") Problem problem);

    /**
     * Count submissions by problem and status
     */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem = :problem AND s.status = :status")
    long countByProblemAndStatus(@Param("problem") Problem problem, @Param("status") Submission.Status status);

    // ✅ ALTERNATIVE: You can also use these entity-based methods
    long countByProblemId(Long problemId);
    long countByProblemIdAndStatus(Long problemId, Submission.Status status);


    // Add these methods to your existing SubmissionRepository.java

    /**
     * Count distinct problems solved by user with specific status
     */
    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.username = :username AND s.status = :status")
    Long countDistinctProblemsByUserUsernameAndStatus(@Param("username") String username, @Param("status") Status status);

    /**
     * Get accepted submissions with problems for difficulty stats
     */
    @Query("SELECT s FROM Submission s JOIN FETCH s.problem WHERE s.user.username = :username AND s.status = 'ACCEPTED'")
    List<Submission> findAcceptedSubmissionsWithProblemsByUsername(@Param("username") String username);

    /**
     * Get language statistics for user
     */
    @Query("SELECT l.displayName, COUNT(s) FROM Submission s JOIN s.language l WHERE s.user.username = :username GROUP BY l.displayName ORDER BY COUNT(s) DESC")
    List<Object[]> getLanguageStatsByUsername(@Param("username") String username);

    /**
     * Get activity data for user within date range
     */
    @Query("SELECT CAST(s.createdAt AS date), COUNT(s) FROM Submission s WHERE s.user.username = :username AND s.createdAt BETWEEN :startDate AND :endDate GROUP BY CAST(s.createdAt AS date) ORDER BY CAST(s.createdAt AS date)")
    List<Object[]> getActivityDataByUsername(@Param("username") String username, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get recent submission dates for streak calculation
     */
    @Query("SELECT DISTINCT CAST(s.createdAt AS date) FROM Submission s WHERE s.user.username = :username AND s.createdAt >= :sinceDate ORDER BY CAST(s.createdAt AS date) DESC")
    List<LocalDate> getRecentSubmissionDatesByUsername(@Param("username") String username, @Param("sinceDate") LocalDateTime sinceDate);

}
