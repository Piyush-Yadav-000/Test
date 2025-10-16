// ProblemTagRepository.java - Create this file
package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.ProblemTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemTagRepository extends JpaRepository<ProblemTag, Long> {

    Optional<ProblemTag> findByName(String name);

    List<ProblemTag> findByIsActiveTrue();

    List<ProblemTag> findByCategoryAndIsActiveTrue(ProblemTag.TagCategory category);

    Page<ProblemTag> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT pt FROM ProblemTag pt WHERE pt.isActive = true AND " +
            "(LOWER(pt.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(pt.displayName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ProblemTag> searchTags(@Param("search") String search);

    @Query("SELECT pt FROM ProblemTag pt JOIN pt.problems p WHERE p.id = :problemId")
    List<ProblemTag> findByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT pt, COUNT(p) as problemCount FROM ProblemTag pt " +
            "LEFT JOIN pt.problems p WHERE pt.isActive = true " +
            "GROUP BY pt ORDER BY problemCount DESC")
    List<Object[]> findTagsWithProblemCount();

    List<ProblemTag> findByOrderBySortOrderAsc();
}
