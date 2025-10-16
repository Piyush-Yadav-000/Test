package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {

    // FIXED: Use 'active' field name instead of 'isActive'
    List<Language> findByActiveTrueOrderBySortOrder();

    Optional<Language> findByNameAndActiveTrue(String name);

    List<Language> findByDisplayNameContainingIgnoreCaseAndActiveTrue(String displayName);
}