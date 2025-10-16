// ContestRepository.java - Create this file
package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.Contest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    Page<Contest> findByIsActiveTrueOrderByStartTimeDesc(Pageable pageable);

    Page<Contest> findByStatusAndIsActiveTrueOrderByStartTimeDesc(Contest.ContestStatus status, Pageable pageable);

    Page<Contest> findByTypeAndIsActiveTrueOrderByStartTimeDesc(Contest.ContestType type, Pageable pageable);

    @Query("SELECT c FROM Contest c WHERE c.isActive = true AND c.status = :status")
    List<Contest> findByStatus(@Param("status") Contest.ContestStatus status);

    @Query("SELECT c FROM Contest c WHERE c.isActive = true AND c.startTime <= :now AND c.endTime >= :now")
    List<Contest> findLiveContests(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Contest c WHERE c.isActive = true AND c.startTime > :now")
    List<Contest> findUpcomingContests(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Contest c WHERE c.isActive = true AND c.endTime < :now")
    List<Contest> findPastContests(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Contest c WHERE c.isActive = true AND " +
            "(LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contest> searchContests(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Contest c JOIN c.participants cp WHERE cp.user.username = :username")
    Page<Contest> findContestsByParticipant(@Param("username") String username, Pageable pageable);

    @Query("SELECT COUNT(cp) FROM ContestParticipation cp WHERE cp.contest.id = :contestId")
    Integer countParticipants(@Param("contestId") Long contestId);

    List<Contest> findByCreatedBy_UsernameOrderByCreatedAtDesc(String username);
}
