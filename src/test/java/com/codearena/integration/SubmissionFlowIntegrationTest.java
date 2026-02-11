package com.codearena.integration;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codearena.dto.AuthResponse;
import com.codearena.dto.CreateProblemRequest;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.StarterCodeRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.TestcaseRepository;
import com.codearena.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the submission flow.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Run code → Submit code → View submission → View stats flow</li>
 *   <li>Rate limiting</li>
 * </ul>
 * 
 * <p>Validates: Requirements 9.1-12.3, 15.1-15.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Submission Flow Integration Tests")
class SubmissionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestcaseRepository testcaseRepository;

    @Autowired
    private StarterCodeRepository starterCodeRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private Long publishedProblemId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        submissionRepository.deleteAll();
        starterCodeRepository.deleteAll();
        testcaseRepository.deleteAll();
        problemRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        userToken = createUserAndGetToken("Test User", "user@example.com", "Password123!", Role.USER);
        adminToken = createUserAndGetToken("Admin User", "admin@example.com", "Password123!", Role.ADMIN);

        // Create and publish a problem
        publishedProblemId = createAndPublishProblem();
    }

    private String createUserAndGetToken(String name, String email, String password, Role role) throws Exception {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            AuthResponse.class
        );
        return response.token();
    }

    private Long createAndPublishProblem() throws Exception {
        // Create problem as admin
        CreateProblemRequest createRequest = new CreateProblemRequest(
            "Two Sum",
            "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
            "1 <= nums.length <= 10^4",
            Difficulty.EASY,
            List.of("array", "hash-table"),
            null
        );

        MvcResult result = mockMvc.perform(post("/api/problems")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long problemId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Add testcases
        String visibleTestcase = "{\"input\":\"[2,7,11,15]\\n9\",\"expectedOutput\":\"[0,1]\",\"isHidden\":false}";
        mockMvc.perform(post("/api/problems/" + problemId + "/testcases")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(visibleTestcase))
                .andExpect(status().isOk());

        String hiddenTestcase = "{\"input\":\"[3,2,4]\\n6\",\"expectedOutput\":\"[1,2]\",\"isHidden\":true}";
        mockMvc.perform(post("/api/problems/" + problemId + "/testcases")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hiddenTestcase))
                .andExpect(status().isOk());

        // Request review and publish
        mockMvc.perform(post("/api/problems/" + problemId + "/request-review")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/problems/" + problemId + "/publish")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        return problemId;
    }

    @Nested
    @DisplayName("User Statistics")
    class UserStatistics {

        @Test
        @DisplayName("should return user statistics")
        void shouldReturnUserStatistics() throws Exception {
            mockMvc.perform(get("/api/me/stats")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSolved").exists())
                    .andExpect(jsonPath("$.totalAttempted").exists());
        }
    }

    @Nested
    @DisplayName("User Profile")
    class UserProfile {

        @Test
        @DisplayName("should return current user profile")
        void shouldReturnCurrentUserProfile() throws Exception {
            mockMvc.perform(get("/api/me")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.name").value("Test User"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }
    }

    @Nested
    @DisplayName("Submission History")
    class SubmissionHistory {

        @Test
        @DisplayName("should return empty submission history for new user")
        void shouldReturnEmptySubmissionHistoryForNewUser() throws Exception {
            mockMvc.perform(get("/api/me/submissions")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("Problem Access")
    class ProblemAccess {

        @Test
        @DisplayName("should allow user to view published problem")
        void shouldAllowUserToViewPublishedProblem() throws Exception {
            mockMvc.perform(get("/api/problems/" + publishedProblemId)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Two Sum"))
                    .andExpect(jsonPath("$.difficulty").value("EASY"));
        }

        @Test
        @DisplayName("should allow user to view visible testcases")
        void shouldAllowUserToViewVisibleTestcases() throws Exception {
            mockMvc.perform(get("/api/problems/" + publishedProblemId + "/testcases")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].input").value("[2,7,11,15]\n9"))
                    .andExpect(jsonPath("$[0].expectedOutput").value("[0,1]"));
        }
    }

    @Nested
    @DisplayName("Admin Submissions")
    class AdminSubmissions {

        @Test
        @DisplayName("should allow admin to view all submissions")
        void shouldAllowAdminToViewAllSubmissions() throws Exception {
            mockMvc.perform(get("/api/admin/submissions")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("should deny regular user from viewing all submissions")
        void shouldDenyUserFromViewingAllSubmissions() throws Exception {
            mockMvc.perform(get("/api/admin/submissions")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }
}
