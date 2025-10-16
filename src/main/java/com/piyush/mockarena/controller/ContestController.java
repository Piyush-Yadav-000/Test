package com.piyush.mockarena.controller;

import com.piyush.mockarena.service.ContestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
@Slf4j
public class ContestController {

    private final ContestService contestService;

    // FIXED: Change return type from Page<ContestResponse> to Map<String, Object>
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllContests(
            Pageable pageable,
            @RequestParam(required = false) Object status) {

        try {
            Map<String, Object> contests = contestService.getAllContests(pageable, status);
            return ResponseEntity.ok(contests);

        } catch (Exception e) {
            log.error("Error fetching contests", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error fetching contests: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getContestById(@PathVariable Long id) {
        try {
            Map<String, Object> contest = contestService.getContestById(id);
            return ResponseEntity.ok(contest);

        } catch (Exception e) {
            log.error("Error fetching contest: {}", id, e);
            return ResponseEntity.status(404).body(
                    Map.of(
                            "message", "Contest not found with id: " + id,
                            "success", false
                    )
            );
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createContest(
            @RequestBody Map<String, Object> contestData,
            Authentication authentication) {

        try {
            Map<String, Object> result = contestService.createContest(contestData, authentication.getName());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error creating contest", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error creating contest: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<Map<String, Object>> registerForContest(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            Map<String, Object> result = contestService.registerForContest(id, authentication.getName());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error registering for contest", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error registering for contest: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<?> getContestLeaderboard(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contestService.getContestLeaderboard(id));

        } catch (Exception e) {
            log.error("Error fetching leaderboard", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error fetching leaderboard: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getContestStats(@PathVariable Long id) {
        try {
            Map<String, Object> stats = contestService.getContestStats(id);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching contest stats", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error fetching contest stats: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startContest(@PathVariable Long id) {
        try {
            contestService.startContest(id);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Contest started successfully!",
                            "contestId", id,
                            "success", true
                    )
            );

        } catch (Exception e) {
            log.error("Error starting contest", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error starting contest: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Map<String, Object>> endContest(@PathVariable Long id) {
        try {
            contestService.endContest(id);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Contest ended successfully!",
                            "contestId", id,
                            "success", true
                    )
            );

        } catch (Exception e) {
            log.error("Error ending contest", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error ending contest: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyContests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Map<String, Object> contests = contestService.getUserContests(authentication.getName(), page, size);
            return ResponseEntity.ok(contests);

        } catch (Exception e) {
            log.error("Error fetching user contests", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error fetching user contests: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcomingContests(Pageable pageable) {
        try {
            Map<String, Object> contests = contestService.getAllContests(pageable, "UPCOMING");
            return ResponseEntity.ok(contests);

        } catch (Exception e) {
            log.error("Error fetching upcoming contests", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error fetching upcoming contests: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> getLiveContests(Pageable pageable) {
        try {
            Map<String, Object> contests = contestService.getAllContests(pageable, "LIVE");
            return ResponseEntity.ok(contests);

        } catch (Exception e) {
            log.error("Error fetching live contests", e);
            return ResponseEntity.status(500).body(
                    Map.of(
                            "message", "Error fetching live contests: " + e.getMessage(),
                            "success", false
                    )
            );
        }
    }
}
