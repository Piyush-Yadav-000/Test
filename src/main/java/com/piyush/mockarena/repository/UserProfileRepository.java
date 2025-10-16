// UserProfileRepository.java - Create this file
package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByUserUsername(String username);

    @Query("SELECT up FROM UserProfile up WHERE up.isPublicProfile = true")
    Page<UserProfile> findPublicProfiles(Pageable pageable);

    @Query("SELECT up FROM UserProfile up WHERE up.contestRating > 0 ORDER BY up.contestRating DESC")
    Page<UserProfile> findByContestRatingOrderByContestRatingDesc(Pageable pageable);

    @Query("SELECT up FROM UserProfile up WHERE up.totalProblemsSolved > 0 ORDER BY up.totalProblemsSolved DESC")
    Page<UserProfile> findByTotalProblemsOrderByTotalProblemsDesc(Pageable pageable);

    @Query("SELECT up FROM UserProfile up WHERE up.isPublicProfile = true AND " +
            "(LOWER(up.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(up.user.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserProfile> searchPublicProfiles(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.contestRating > :rating")
    Long countByContestRatingGreaterThan(@Param("rating") Integer rating);

    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.totalProblemsSolved > :count")
    Long countByTotalProblemsGreaterThan(@Param("count") Integer count);

    @Query("SELECT up FROM UserProfile up WHERE up.lastActiveDate >= :date")
    List<UserProfile> findActiveUsersAfter(@Param("date") LocalDateTime date);

    @Query("SELECT AVG(up.contestRating) FROM UserProfile up WHERE up.contestRating > 0")
    Double getAverageContestRating();

    @Query("SELECT up FROM UserProfile up WHERE up.streakCount > 0 ORDER BY up.streakCount DESC")
    List<UserProfile> findTopStreakUsers(Pageable pageable);
}
