// ContestParticipationRepository.java - Create this file
package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.ContestParticipation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, Long> {

    Optional<ContestParticipation> findByContestIdAndUserId(Long contestId, Long userId);

    Optional<ContestParticipation> findByContestIdAndUserUsername(Long contestId, String username);

    List<ContestParticipation> findByContestIdOrderByTotalScoreDescPenaltyTimeAsc(Long contestId);

    Page<ContestParticipation> findByUserUsernameOrderByRegistrationTimeDesc(String username, Pageable pageable);

    @Query("SELECT cp FROM ContestParticipation cp WHERE cp.contest.id = :contestId " +
            "ORDER BY cp.totalScore DESC, cp.penaltyTime ASC, cp.registrationTime ASC")
    List<ContestParticipation> findLeaderboard(@Param("contestId") Long contestId);

    @Query("SELECT cp FROM ContestParticipation cp WHERE cp.contest.id = :contestId AND cp.status = 'ACTIVE' " +
            "ORDER BY cp.totalScore DESC, cp.penaltyTime ASC")
    List<ContestParticipation> findLiveLeaderboard(@Param("contestId") Long contestId);

    @Query("SELECT COUNT(cp) FROM ContestParticipation cp WHERE cp.contest.id = :contestId")
    Integer countByContestId(@Param("contestId") Long contestId);

    @Query("SELECT COUNT(cp) FROM ContestParticipation cp WHERE cp.user.username = :username")
    Integer countByUsername(@Param("username") String username);

    List<ContestParticipation> findByUserIdOrderByRegistrationTimeDesc(Long userId);

    @Query("SELECT cp FROM ContestParticipation cp WHERE cp.contest.id = :contestId AND cp.finalRank IS NOT NULL " +
            "ORDER BY cp.finalRank ASC")
    List<ContestParticipation> findFinalRankings(@Param("contestId") Long contestId);

    boolean existsByContestIdAndUserId(Long contestId, Long userId);
}
