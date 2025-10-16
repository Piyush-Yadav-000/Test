package com.piyush.mockarena.service;

import com.piyush.mockarena.entity.*;
import com.piyush.mockarena.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final LanguageRepository languageRepository;  // ✅ ADDED
    private final LanguageService languageService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Initializing Phase 3 data...");

        initializeLanguages();
        initializeLanguageTemplates();  // ✅ NEW: Initialize templates
        initializeUsers();
        initializeProblemsWithTestCases();

        log.info("✅ Phase 3 data initialization complete!");
    }

    private void initializeLanguages() {
        log.info("Initializing programming languages...");
        languageService.initializePopularLanguages();
    }

    /**
     * ✅ NEW: Initialize language-specific code templates
     */
    private void initializeLanguageTemplates() {
        log.info("📝 Initializing language templates...");

        // Update language templates if they don't exist
        updateLanguageTemplate(50, "c", generateCTemplate());           // C
        updateLanguageTemplate(54, "cpp", generateCppTemplate());       // C++
        updateLanguageTemplate(62, "java", generateJavaTemplate());     // Java
        updateLanguageTemplate(71, "python", generatePythonTemplate()); // Python3
        updateLanguageTemplate(63, "javascript", generateJsTemplate()); // JavaScript
        updateLanguageTemplate(78, "kotlin", generateKotlinTemplate()); // Kotlin
        updateLanguageTemplate(51, "csharp", generateCSharpTemplate()); // C#
        updateLanguageTemplate(60, "go", generateGoTemplate());         // Go
        updateLanguageTemplate(68, "php", generatePhpTemplate());       // PHP
        updateLanguageTemplate(72, "ruby", generateRubyTemplate());     // Ruby

        log.info("✅ Language templates initialized");
    }

    private void updateLanguageTemplate(Integer languageId, String languageName, String template) {
        try {
            var languageOpt = languageRepository.findById(languageId);
            if (languageOpt.isPresent()) {
                Language language = languageOpt.get();
                if (language.getCodeTemplate() == null || language.getCodeTemplate().trim().isEmpty()) {
                    language.setCodeTemplate(template);
                    languageRepository.save(language);
                    log.info("✅ Set template for: {}", language.getDisplayName());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to set template for language ID {}: {}", languageId, e.getMessage());
        }
    }

    private void initializeUsers() {
        // Create admin if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@mockarena.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            admin.setCreatedAt(LocalDateTime.now());  // ✅ ADDED
            admin.setUpdatedAt(LocalDateTime.now());  // ✅ ADDED
            userRepository.save(admin);
            log.info("Created admin user");
        }

        // Create test user if not exists
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("test@mockarena.com");
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setRole(User.Role.USER);
            testUser.setCreatedAt(LocalDateTime.now());  // ✅ ADDED
            testUser.setUpdatedAt(LocalDateTime.now());  // ✅ ADDED
            userRepository.save(testUser);
            log.info("Created test user");
        }
    }

    private void initializeProblemsWithTestCases() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) return;

        // Create problems if none exist
        if (problemRepository.count() == 0) {

            // ✅ FIXED: Two Sum with correct field names
            Problem twoSum = new Problem();
            twoSum.setTitle("Two Sum");
            twoSum.setSlug("two-sum");
            twoSum.setDescription("""
                Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.

                You may assume that each input would have exactly one solution, and you may not use the same element twice.

                You can return the answer in any order.

                **Example 1:**
                ```
                Input: nums = , target = 9[1][2]
                Output:[3]
                Explanation: Because nums + nums == 9, we return.[3]
                ```

                **Example 2:**
                ```
                Input: nums = , target = 6[1]
                Output:[1][3]
                ```

                **Example 3:**
                ```
                Input: nums = , target = 6
                Output:[3]
                ```

                **Constraints:**
                - 2 <= nums.length <= 10^4
                - -10^9 <= nums[i] <= 10^9
                - -10^9 <= target <= 10^9
                - Only one valid answer exists.
                """);
            twoSum.setDifficulty(Problem.Difficulty.EASY);
            twoSum.setCreatedBy(admin);
            twoSum.setTimeLimitMs(1000);       // ✅ FIXED: Use correct field name
            twoSum.setMemoryLimitMb(256);      // ✅ FIXED: Use correct field name
            twoSum.setSampleInput("[2,7,11,15]\n9");
            twoSum.setSampleOutput("[0,1]");
            twoSum.setCreatedAt(LocalDateTime.now());
            twoSum.setUpdatedAt(LocalDateTime.now());
            twoSum = problemRepository.save(twoSum);

            // Add test cases for Two Sum
            createTestCase(twoSum, "[2,7,11,15]\n9", "[0,1]", TestCase.Type.PUBLIC, 1);
            createTestCase(twoSum, "[3,2,4]\n6", "[1,2]", TestCase.Type.PUBLIC, 2);
            createTestCase(twoSum, "[3,3]\n6", "[0,1]", TestCase.Type.HIDDEN, 3);
            createTestCase(twoSum, "[1,2,3,4,5]\n8", "[2,4]", TestCase.Type.HIDDEN, 4);

            log.info("Created Two Sum problem with test cases");

            // ✅ FIXED: Hello World problem
            Problem helloWorld = new Problem();
            helloWorld.setTitle("Hello World");
            helloWorld.setSlug("hello-world");
            helloWorld.setDescription("""
                Write a program that outputs "Hello, World!" to the standard output.

                This is a simple problem to test the code execution system.
                
                **Expected Output:**
                ```
                Hello, World!
                ```
                """);
            helloWorld.setDifficulty(Problem.Difficulty.EASY);
            helloWorld.setCreatedBy(admin);
            helloWorld.setTimeLimitMs(1000);    // ✅ FIXED
            helloWorld.setMemoryLimitMb(256);   // ✅ FIXED
            helloWorld.setSampleInput("");
            helloWorld.setSampleOutput("Hello, World!");
            helloWorld.setCreatedAt(LocalDateTime.now());
            helloWorld.setUpdatedAt(LocalDateTime.now());
            helloWorld = problemRepository.save(helloWorld);

            // Add test case for Hello World
            createTestCase(helloWorld, "", "Hello, World!", TestCase.Type.PUBLIC, 1);

            log.info("Created Hello World problem with test case");

            // ✅ NEW: Add Two Numbers (Linked List) problem
            Problem addTwoNumbers = new Problem();
            addTwoNumbers.setTitle("Add Two Numbers");
            addTwoNumbers.setSlug("add-two-numbers");
            addTwoNumbers.setDescription("""
                You are given two non-empty linked lists representing two non-negative integers. The digits are stored in reverse order, and each of their nodes contains a single digit. Add the two numbers and return the sum as a linked list.

                You may assume the two numbers do not contain any leading zero, except the number 0 itself.

                **Example 1:**
                ```
                Input: l1 = , l2 =[1]
                Output:[2][4]
                Explanation: 342 + 465 = 807.
                ```

                **Example 2:**
                ```
                Input: l1 = , l2 = 
                Output: 
                ```

                **Constraints:**
                - The number of nodes in each linked list is in the range [1, 100].
                - 0 <= Node.val <= 9
                """);
            addTwoNumbers.setDifficulty(Problem.Difficulty.MEDIUM);
            addTwoNumbers.setCreatedBy(admin);
            addTwoNumbers.setTimeLimitMs(1000);    // ✅ FIXED
            addTwoNumbers.setMemoryLimitMb(256);   // ✅ FIXED
            addTwoNumbers.setSampleInput("[2,4,3]\n[5,6,4]");
            addTwoNumbers.setSampleOutput("[7,0,8]");
            addTwoNumbers.setCreatedAt(LocalDateTime.now());
            addTwoNumbers.setUpdatedAt(LocalDateTime.now());
            addTwoNumbers = problemRepository.save(addTwoNumbers);

            // Add test cases for Add Two Numbers
            createTestCase(addTwoNumbers, "[2,4,3]\n[5,6,4]", "[7,0,8]", TestCase.Type.PUBLIC, 1);
            createTestCase(addTwoNumbers, "[0]\n[0]", "[0]", TestCase.Type.PUBLIC, 2);
            createTestCase(addTwoNumbers, "[9,9,9,9,9,9,9]\n[9,9,9,9]", "[8,9,9,9,0,0,0,1]", TestCase.Type.HIDDEN, 3);

            log.info("Created Add Two Numbers problem with test cases");

            // ✅ NEW: Longest Substring problem
            Problem longestSubstring = new Problem();
            longestSubstring.setTitle("Longest Substring Without Repeating Characters");
            longestSubstring.setSlug("longest-substring-without-repeating-characters");
            longestSubstring.setDescription("""
                Given a string `s`, find the length of the longest substring without repeating characters.

                **Example 1:**
                ```
                Input: s = "abcabcbb"
                Output: 3
                Explanation: The answer is "abc", with the length of 3.
                ```

                **Example 2:**
                ```
                Input: s = "bbbbb"
                Output: 1
                Explanation: The answer is "b", with the length of 1.
                ```

                **Example 3:**
                ```
                Input: s = "pwwkew"
                Output: 3
                Explanation: The answer is "wke", with the length of 3.
                ```

                **Constraints:**
                - 0 <= s.length <= 5 * 10^4
                - s consists of English letters, digits, symbols and spaces.
                """);
            longestSubstring.setDifficulty(Problem.Difficulty.MEDIUM);
            longestSubstring.setCreatedBy(admin);
            longestSubstring.setTimeLimitMs(1000);     // ✅ FIXED
            longestSubstring.setMemoryLimitMb(256);    // ✅ FIXED
            longestSubstring.setSampleInput("abcabcbb");
            longestSubstring.setSampleOutput("3");
            longestSubstring.setCreatedAt(LocalDateTime.now());
            longestSubstring.setUpdatedAt(LocalDateTime.now());
            longestSubstring = problemRepository.save(longestSubstring);

            // Add test cases
            createTestCase(longestSubstring, "abcabcbb", "3", TestCase.Type.PUBLIC, 1);
            createTestCase(longestSubstring, "bbbbb", "1", TestCase.Type.PUBLIC, 2);
            createTestCase(longestSubstring, "pwwkew", "3", TestCase.Type.HIDDEN, 3);
            createTestCase(longestSubstring, "", "0", TestCase.Type.HIDDEN, 4);

            log.info("Created Longest Substring problem with test cases");
        }
    }

    private void createTestCase(Problem problem, String input, String expectedOutput, TestCase.Type type, int sortOrder) {
        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        testCase.setType(type);
        testCase.setSortOrder(sortOrder);
        testCase.setActive(true);  // ✅ Uses isActive field (boolean)
        testCase.setCreatedAt(LocalDateTime.now());  // ✅ Only set createdAt (no updatedAt)
        testCaseRepository.save(testCase);
    }


    // ✅ LANGUAGE TEMPLATE GENERATORS

    private String generateCTemplate() {
        return """
            #include <stdio.h>
            #include <stdlib.h>
            
            int main() {
                // Write your code here
                
                return 0;
            }
            """;
    }

    private String generateCppTemplate() {
        return """
            #include <iostream>
            #include <vector>
            #include <string>
            using namespace std;
            
            class Solution {
            public:
                // TODO: Implement your solution here
                
            };
            
            int main() {
                Solution sol;
                // Test your solution here
                return 0;
            }
            """;
    }

    private String generateJavaTemplate() {
        return """
            import java.util.*;
            
            class Solution {
                // TODO: Implement your solution here
                
            }
            
            public class Main {
                public static void main(String[] args) {
                    Solution sol = new Solution();
                    // Test your solution here
                }
            }
            """;
    }

    private String generatePythonTemplate() {
        return """
            class Solution:
                def solve(self):
                    # TODO: Implement your solution here
                    pass
            
            # Test your solution here
            sol = Solution()
            # print(sol.solve())
            """;
    }

    private String generateJsTemplate() {
        return """
            /**
             * @return {void}
             */
            var solve = function() {
                // TODO: Implement your solution here
                
            };
            
            // Test your solution here
            // console.log(solve());
            """;
    }

    private String generateKotlinTemplate() {
        return """
            class Solution {
                fun solve(): Unit {
                    // TODO: Implement your solution here
                    
                }
            }
            
            fun main() {
                val sol = Solution()
                // Test your solution here
            }
            """;
    }

    private String generateCSharpTemplate() {
        return """
            using System;
            using System.Collections.Generic;
            
            public class Solution {
                // TODO: Implement your solution here
                
            }
            
            public class Program {
                public static void Main() {
                    Solution sol = new Solution();
                    // Test your solution here
                }
            }
            """;
    }

    private String generateGoTemplate() {
        return """
            package main
            
            import "fmt"
            
            func solve() {
                // TODO: Implement your solution here
                
            }
            
            func main() {
                // Test your solution here
                solve()
            }
            """;
    }

    private String generatePhpTemplate() {
        return """
            <?php
            
            class Solution {
                public function solve() {
                    // TODO: Implement your solution here
                    
                }
            }
            
            // Test your solution here
            $sol = new Solution();
            // echo $sol->solve();
            
            ?>
            """;
    }

    private String generateRubyTemplate() {
        return """
            class Solution
                def solve
                    # TODO: Implement your solution here
                    
                end
            end
            
            # Test your solution here
            sol = Solution.new
            # puts sol.solve
            """;
    }
}
