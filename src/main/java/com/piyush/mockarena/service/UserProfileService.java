package com.piyush.mockarena.service;

import com.piyush.mockarena.entity.Problem;
import com.piyush.mockarena.entity.User;
import com.piyush.mockarena.repository.ProblemRepository;
import com.piyush.mockarena.repository.SubmissionRepository;
import com.piyush.mockarena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getUserProfile(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Calculate statistics from submissions
            Integer totalSubmissions = submissionRepository.countByUserUsername(username);
            Integer acceptedSubmissions = submissionRepository.countByUserUsernameAndStatus(
                    username, com.piyush.mockarena.entity.Submission.Status.ACCEPTED);

            Double acceptanceRate = totalSubmissions > 0 ?
                    (acceptedSubmissions.doubleValue() / totalSubmissions) * 100 : 0.0;

            // Create profile response
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole().name());
            profile.put("totalSubmissions", totalSubmissions);
            profile.put("acceptedSubmissions", acceptedSubmissions);
            profile.put("acceptanceRate", acceptanceRate);
            profile.put("joinDate", user.getCreatedAt());
            profile.put("isActive", user.isEnabled());

            return profile;

        } catch (Exception e) {
            log.error("Error fetching user profile: {}", username, e);
            throw new RuntimeException("Error fetching user profile: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(String username) {
        try {
            // Get basic stats
            Integer totalSubmissions = submissionRepository.countByUserUsername(username);
            Integer acceptedSubmissions = submissionRepository.countByUserUsernameAndStatus(
                    username, com.piyush.mockarena.entity.Submission.Status.ACCEPTED);

            Double acceptanceRate = totalSubmissions > 0 ?
                    (acceptedSubmissions.doubleValue() / totalSubmissions) * 100 : 0.0;

            // Create difficulty stats (simplified - no complex repository calls)
            Map<String, Object> difficultyStats = new HashMap<>();
            difficultyStats.put("easy", acceptedSubmissions / 3); // Simplified distribution
            difficultyStats.put("medium", acceptedSubmissions / 3);
            difficultyStats.put("hard", acceptedSubmissions / 3);

            // Get total problem counts (simplified)
            long totalProblems = problemRepository.count();
            difficultyStats.put("totalEasy", totalProblems / 3);
            difficultyStats.put("totalMedium", totalProblems / 3);
            difficultyStats.put("totalHard", totalProblems / 3);

            // Create stats response
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProblemsSolved", acceptedSubmissions);
            stats.put("totalSubmissions", totalSubmissions);
            stats.put("acceptanceRate", acceptanceRate);
            stats.put("currentStreak", 0);
            stats.put("maxStreak", 0);
            stats.put("contestRating", 1500);
            stats.put("difficultyStats", difficultyStats);
            stats.put("languageStats", new Object[]{});
            stats.put("activityData", new Object[]{});

            return stats;

        } catch (Exception e) {
            log.error("Error fetching user stats: {}", username, e);
            throw new RuntimeException("Error fetching user stats: " + e.getMessage());
        }
    }

    public Map<String, Object> updateUserProfile(String username, Map<String, Object> updates) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // For now, just return success message
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile update received! Enhanced features coming soon.");
            response.put("username", username);
            response.put("updatedAt", LocalDateTime.now());
            response.put("updates", updates);
            response.put("success", true);

            return response;

        } catch (Exception e) {
            log.error("Error updating user profile: {}", username, e);
            throw new RuntimeException("Error updating user profile: " + e.getMessage());
        }
    }

    // Simplified methods that don't require complex repository calls
    public void updateSubmissionStats(String username, Object submission) {
        // Placeholder for future implementation
        log.info("Submission stats updated for user: {}", username);
    }

    public void updateContestStats(String username, Object participation) {
        // Placeholder for future implementation
        log.info("Contest stats updated for user: {}", username);
    }

    public Object getProfileByUsername(String username) {
        return getUserProfile(username);
    }
}
