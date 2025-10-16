package com.piyush.mockarena.controller;

import com.piyush.mockarena.entity.User;
import com.piyush.mockarena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getEmail() == null) {
            return ResponseEntity.badRequest().body("Username and email required");
        }

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("MockArena API is working! 🚀");
    }

    // NEW: Add the missing /me endpoint
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            // Get user from database
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create response with user details
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getId());
            userResponse.put("username", user.getUsername());
            userResponse.put("email", user.getEmail());
            userResponse.put("role", user.getRole().name());
            userResponse.put("enabled", user.isEnabled());
            userResponse.put("createdAt", user.getCreatedAt());
            userResponse.put("updatedAt", user.getUpdatedAt());

            log.info("User details fetched for: {}", user.getUsername());
            return ResponseEntity.ok(userResponse);

        } catch (Exception e) {
            log.error("Error fetching user details for: {}", authentication.getName(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error fetching user details: " + e.getMessage());
            errorResponse.put("success", false);
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // BONUS: Add profile test endpoint
    @GetMapping("/profile-test")
    public ResponseEntity<?> profileTest(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("username", authentication.getName());
        response.put("message", "User authentication working!");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
