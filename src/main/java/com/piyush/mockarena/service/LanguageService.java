package com.piyush.mockarena.service;

import com.piyush.mockarena.entity.Language;
import com.piyush.mockarena.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final Judge0Service judge0Service;

    // FIXED: Use 'active' field consistently
    public List<Language> getSupportedLanguages() {
        return languageRepository.findByActiveTrueOrderBySortOrder();
    }

    public Language getLanguageById(Integer id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Language not found"));
    }

    public List<Language> searchLanguages(String query) {
        return languageRepository.findByDisplayNameContainingIgnoreCaseAndActiveTrue(query);
    }

    /**
     * Initialize popular languages for immediate use
     */
    @Transactional
    public void initializePopularLanguages() {
        log.info("Initializing popular programming languages");

        // Popular languages with Judge0 IDs
        Language[] popularLanguages = {
                new Language(71, "python", "Python 3.8.1", "py"),
                new Language(62, "java", "Java (OpenJDK 13.0.1)", "java"),
                new Language(54, "cpp", "C++ (GCC 9.2.0)", "cpp"),
                new Language(63, "javascript", "JavaScript (Node.js 12.14.0)", "js"),
                new Language(50, "c", "C (GCC 9.2.0)", "c"),
                new Language(51, "csharp", "C# (Mono 6.6.0.161)", "cs"),
                new Language(60, "go", "Go (1.13.5)", "go"),
                new Language(68, "php", "PHP (7.4.1)", "php"),
                new Language(72, "ruby", "Ruby (2.7.0)", "rb"),
                new Language(73, "rust", "Rust (1.40.0)", "rs")
        };

        for (Language lang : popularLanguages) {
            if (!languageRepository.existsById(lang.getId())) {
                lang.setCodeTemplate("// Write your solution here");
                lang.setSortOrder(Arrays.asList(popularLanguages).indexOf(lang));
                languageRepository.save(lang);
                log.info("Added language: {} (ID: {})", lang.getDisplayName(), lang.getId());
            }
        }

        log.info("Popular languages initialization complete");
    }

    /**
     * Refresh languages from Judge0 API - SIMPLIFIED
     */
    public void refreshLanguagesFromJudge0() {
        log.info("Refreshing languages from Judge0");

        try {
            Judge0Service.Judge0Language[] judge0Languages = judge0Service.getSupportedLanguages().block();

            if (judge0Languages != null) {
                for (Judge0Service.Judge0Language j0Lang : judge0Languages) {
                    if (!languageRepository.existsById(j0Lang.getId())) {
                        Language newLang = new Language();
                        newLang.setId(j0Lang.getId());
                        newLang.setName(j0Lang.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", ""));
                        newLang.setDisplayName(j0Lang.getName());
                        newLang.setActive(false); // New languages inactive by default
                        newLang.setCodeTemplate("// Write your solution here");
                        languageRepository.save(newLang);
                        log.info("Added new language from Judge0: {}", j0Lang.getName());
                    }
                }
                log.info("Language refresh complete. Total languages: {}", judge0Languages.length);
            }

        } catch (Exception e) {
            log.error("Failed to refresh languages from Judge0: {}", e.getMessage());
        }
    }
}