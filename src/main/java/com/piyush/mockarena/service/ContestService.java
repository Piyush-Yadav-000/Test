package com.piyush.mockarena.service;

import com.piyush.mockarena.entity.User;
import com.piyush.mockarena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContestService {

    private final UserRepository userRepository;

    // FIXED METHOD - Updated to match ContestController parameters
    @Transactional(readOnly = true)
    public Map<String, Object> getAllContests(Pageable pageable, Object status) {
        try {
            // Extract page and size from Pageable
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();

            // Convert status to string if needed
            String statusStr = status != null ? status.toString() : "ALL";

            // For now, return empty contests (no Contest entity implementation yet)
            Map<String, Object> response = new HashMap<>();
            response.put("content", new ArrayList<>());
            response.put("totalElements", 0L);
            response.put("totalPages", 0);
            response.put("size", size);
            response.put("number", page);
            response.put("status", statusStr);
            response.put("message", "Contest system coming soon!");

            log.info("Fetching contests - page: {}, size: {}, status: {}", page, size, statusStr);
            return response;

        } catch (Exception e) {
            log.error("Error fetching contests", e);
            throw new RuntimeException("Error fetching contests: " + e.getMessage());
        }
    }

    // OVERLOADED METHOD - Keep the old signature for backward compatibility
    @Transactional(readOnly = true)
    public Map<String, Object> getAllContests(int page, int size, String status) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("content", new ArrayList<>());
            response.put("totalElements", 0L);
            response.put("totalPages", 0);
            response.put("size", size);
            response.put("number", page);
            response.put("status", status);
            response.put("message", "Contest system coming soon!");

            return response;

        } catch (Exception e) {
            log.error("Error fetching contests", e);
            throw new RuntimeException("Error fetching contests: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getContestById(Long id) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Contest details coming soon!");
            response.put("contestId", id);
            response.put("status", "UPCOMING");

            return response;

        } catch (Exception e) {
            log.error("Error fetching contest: {}", id, e);
            throw new RuntimeException("Contest not found with id: " + id);
        }
    }

    public Map<String, Object> createContest(Map<String, Object> contestData, String creatorUsername) {
        try {
            User creator = userRepository.findByUsername(creatorUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // For now, return success message (no Contest entity implementation yet)
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Contest creation successful!");
            response.put("contestData", contestData);
            response.put("createdBy", creatorUsername);
            response.put("createdAt", LocalDateTime.now());
            response.put("success", true);

            log.info("Contest creation request by: {}", creatorUsername);
            return response;

        } catch (Exception e) {
            log.error("Error creating contest", e);
            throw new RuntimeException("Error creating contest: " + e.getMessage());
        }
    }

    public Map<String, Object> registerForContest(Long contestId, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // For now, return success message
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Contest registration successful!");
            response.put("contestId", contestId);
            response.put("username", username);
            response.put("registeredAt", LocalDateTime.now());
            response.put("success", true);

            log.info("Contest registration: {} for contest: {}", username, contestId);
            return response;

        } catch (Exception e) {
            log.error("Error registering for contest", e);
            throw new RuntimeException("Error registering for contest: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getContestLeaderboard(Long contestId) {
        try {
            // For now, return empty leaderboard
            List<Map<String, Object>> leaderboard = new ArrayList<>();

            Map<String, Object> entry = new HashMap<>();
            entry.put("message", "Contest leaderboard coming soon!");
            entry.put("contestId", contestId);

            leaderboard.add(entry);

            return leaderboard;

        } catch (Exception e) {
            log.error("Error fetching contest leaderboard: {}", contestId, e);
            throw new RuntimeException("Error fetching leaderboard: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getContestStats(Long contestId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("contestId", contestId);
            stats.put("totalParticipants", 0);
            stats.put("activeParticipants", 0);
            stats.put("timeRemaining", 0L);
            stats.put("status", "UPCOMING");
            stats.put("message", "Contest statistics coming soon!");

            return stats;

        } catch (Exception e) {
            log.error("Error fetching contest stats: {}", contestId, e);
            throw new RuntimeException("Error fetching contest stats: " + e.getMessage());
        }
    }

    public void startContest(Long contestId) {
        try {
            log.info("Contest start request for contest: {}", contestId);
            // For now, just log the request

        } catch (Exception e) {
            log.error("Error starting contest: {}", contestId, e);
            throw new RuntimeException("Error starting contest: " + e.getMessage());
        }
    }

    public void endContest(Long contestId) {
        try {
            log.info("Contest end request for contest: {}", contestId);
            // For now, just log the request

        } catch (Exception e) {
            log.error("Error ending contest: {}", contestId, e);
            throw new RuntimeException("Error ending contest: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserContests(String username, int page, int size) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("content", new ArrayList<>());
            response.put("totalElements", 0L);
            response.put("totalPages", 0);
            response.put("size", size);
            response.put("number", page);
            response.put("username", username);
            response.put("message", "User contest history coming soon!");

            return response;

        } catch (Exception e) {
            log.error("Error fetching user contests: {}", username, e);
            throw new RuntimeException("Error fetching user contests: " + e.getMessage());
        }
    }

    // Helper method to update contest statistics (placeholder)
    public void updateContestStats(String username, Object participation) {
        try {
            log.info("Contest stats update for user: {}", username);
            // Placeholder for future implementation

        } catch (Exception e) {
            log.error("Error updating contest stats for user: {}", username, e);
        }
    }
}
