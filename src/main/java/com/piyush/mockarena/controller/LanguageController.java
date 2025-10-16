package com.piyush.mockarena.controller;

import com.piyush.mockarena.entity.Language;
import com.piyush.mockarena.service.LanguageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
@Slf4j
public class LanguageController {

    private final LanguageService languageService;

    /**
     * Get all supported programming languages
     */
    @GetMapping
    public ResponseEntity<List<Language>> getSupportedLanguages() {
        List<Language> languages = languageService.getSupportedLanguages();
        return ResponseEntity.ok(languages);
    }

    /**
     * Get language by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Language> getLanguage(@PathVariable Integer id) {
        Language language = languageService.getLanguageById(id);
        return ResponseEntity.ok(language);
    }

    /**
     * Search languages by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<Language>> searchLanguages(@RequestParam String query) {
        List<Language> languages = languageService.searchLanguages(query);
        return ResponseEntity.ok(languages);
    }

    /**
     * Refresh languages from Judge0 (admin only)
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshLanguages() {
        languageService.refreshLanguagesFromJudge0();
        return ResponseEntity.ok("Languages refreshed from Judge0");
    }
}