package com.piyush.mockarena.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("MockArena health check");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "MockArena",
                "version", "0.0.1-SNAPSHOT"
        ));
    }
}
