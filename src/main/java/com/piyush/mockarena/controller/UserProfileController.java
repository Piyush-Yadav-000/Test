package com.piyush.mockarena.controller;

import com.piyush.mockarena.entity.User;
import com.piyush.mockarena.repository.UserRepository;
import com.piyush.mockarena.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            // Get user from database (existing working code)
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Calculate stats from existing submissions table
            Integer totalSubmissions = submissionRepository.countByUserUsername(authentication.getName());
            Integer acceptedSubmissions = submissionRepository.countByUserUsernameAndStatus(
                    authentication.getName(), com.piyush.mockarena.entity.Submission.Status.ACCEPTED);

            // Calculate acceptance rate
            Double acceptanceRate = totalSubmissions > 0 ?
                    (acceptedSubmissions.doubleValue() / totalSubmissions) * 100 : 0.0;

            // Create profile response using existing data only
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole().name());
            profile.put("fullName", user.getUsername()); // Use username as fullName for now
            profile.put("profilePictureUrl", null);
            profile.put("bio", null);
            profile.put("githubUsername", null);
            profile.put("currentCompany", null);
            profile.put("currentPosition", null);
            profile.put("location", null);
            profile.put("websiteUrl", null);

            // Statistics from existing data
            profile.put("totalProblemsSolved", acceptedSubmissions); // Count of accepted submissions
            profile.put("totalSubmissions", totalSubmissions);
            profile.put("acceptedSubmissions", acceptedSubmissions);
            profile.put("acceptanceRate", acceptanceRate);
            profile.put("contestRating", 1500); // Default rating
            profile.put("maxContestRating", 1500);
            profile.put("contestsParticipated", 0);
            profile.put("globalRank", null);
            profile.put("countryRank", null);
            profile.put("streakCount", 0);
            profile.put("maxStreakCount", 0);
            profile.put("preferredLanguage", "Java");

            // Dates
            profile.put("joinDate", user.getCreatedAt());
            profile.put("lastActiveDate", LocalDateTime.now());
            profile.put("isPublicProfile", true);
            profile.put("isActive", user.isEnabled());

            log.info("Profile fetched successfully for user: {}", authentication.getName());
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error("Error fetching profile for user: {}", authentication.getName(), e);

            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error fetching profile: " + e.getMessage());
            errorResponse.put("success", false);
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/me/stats")
    public ResponseEntity<?> getUserStats(Authentication authentication) {
        try {
            // Get statistics from existing submissions
            Integer totalSubmissions = submissionRepository.countByUserUsername(authentication.getName());
            Integer acceptedSubmissions = submissionRepository.countByUserUsernameAndStatus(
                    authentication.getName(), com.piyush.mockarena.entity.Submission.Status.ACCEPTED);

            Double acceptanceRate = totalSubmissions > 0 ?
                    (acceptedSubmissions.doubleValue() / totalSubmissions) * 100 : 0.0;

            // Create stats response
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProblemsSolved", acceptedSubmissions);
            stats.put("totalSubmissions", totalSubmissions);
            stats.put("acceptedSubmissions", acceptedSubmissions);
            stats.put("acceptanceRate", acceptanceRate);
            stats.put("currentStreak", 0);
            stats.put("maxStreak", 0);
            stats.put("contestRating", 1500);
            stats.put("globalRank", null);

            // Difficulty breakdown (simplified)
            Map<String, Object> difficultyStats = new HashMap<>();
            difficultyStats.put("easy", acceptedSubmissions / 3); // Rough estimate
            difficultyStats.put("medium", acceptedSubmissions / 3);
            difficultyStats.put("hard", acceptedSubmissions / 3);
            difficultyStats.put("totalEasy", 50); // Example totals
            difficultyStats.put("totalMedium", 75);
            difficultyStats.put("totalHard", 25);
            difficultyStats.put("easyPercentage", 33.3);
            difficultyStats.put("mediumPercentage", 33.3);
            difficultyStats.put("hardPercentage", 33.3);
            stats.put("difficultyStats", difficultyStats);

            // Empty for now - no complex queries needed
            stats.put("languageStats", new Object[]{});
            stats.put("activityData", new Object[]{});
            stats.put("recentSubmissions", new Object[]{});
            stats.put("monthlyProgress", new Object[]{});

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching stats for user: {}", authentication.getName(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error fetching stats: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // For now, just return success without saving to new table
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile update received! Enhanced profile features coming soon.");
            response.put("username", user.getUsername());
            response.put("updatedAt", LocalDateTime.now());
            response.put("updates", updates);
            response.put("success", true);

            log.info("Profile update request for user: {} with data: {}", authentication.getName(), updates);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating profile for user: {}", authentication.getName(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error updating profile: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getGlobalLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            // Simple leaderboard from existing users table
            Map<String, Object> leaderboard = new HashMap<>();
            leaderboard.put("content", new Object[]{});
            leaderboard.put("totalElements", 0);
            leaderboard.put("totalPages", 0);
            leaderboard.put("size", size);
            leaderboard.put("number", page);
            leaderboard.put("message", "Leaderboard feature coming soon!");

            return ResponseEntity.ok(leaderboard);

        } catch (Exception e) {
            log.error("Error fetching leaderboard", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error fetching leaderboard: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
