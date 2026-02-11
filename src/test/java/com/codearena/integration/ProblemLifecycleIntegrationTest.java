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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codearena.dto.AuthResponse;
import com.codearena.dto.CreateProblemRequest;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.StarterCodeRepository;
import com.codearena.repository.TestcaseRepository;
import com.codearena.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the problem lifecycle.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Create → Add testcases → Request review → Publish flow</li>
 *   <li>Access control at each stage</li>
 *   <li>Problem state transitions</li>
 * </ul>
 * 
 * <p>Validates: Requirements 6.1-8.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Problem Lifecycle Integration Tests")
class ProblemLifecycleIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private String problemSetterToken;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        starterCodeRepository.deleteAll();
        testcaseRepository.deleteAll();
        problemRepository.deleteAll();
        userRepository.deleteAll();

        // Create problem setter
        problemSetterToken = createUserAndGetToken("Problem Setter", "setter@example.com", "Password123!", Role.PROBLEM_SETTER);
        
        // Create admin
        adminToken = createUserAndGetToken("Admin User", "admin@example.com", "Password123!", Role.ADMIN);
        
        // Create regular user
        userToken = createUserAndGetToken("Regular User", "user@example.com", "Password123!", Role.USER);
    }

    private String createUserAndGetToken(String name, String email, String password, Role role) throws Exception {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);

        // Login to get token
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

    @Nested
    @DisplayName("Complete Problem Lifecycle")
    class CompleteProblemLifecycle {

        @Test
        @DisplayName("should complete create → add testcases → request review → publish flow")
        void shouldCompleteFullProblemLifecycle() throws Exception {
            // Step 1: Create problem (PROBLEM_SETTER)
            CreateProblemRequest createRequest = new CreateProblemRequest(
                "Two Sum",
                "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
                "1 <= nums.length <= 10^4",
                Difficulty.EASY,
                List.of("array", "hash-table"),
                null
            );

            MvcResult createResult = mockMvc.perform(post("/api/problems")
                    .header("Authorization", "Bearer " + problemSetterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Two Sum"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.slug").value("two-sum"))
                    .andReturn();

            // Extract problem ID
            String responseBody = createResult.getResponse().getContentAsString();
            Long problemId = objectMapper.readTree(responseBody).get("id").asLong();

            // Step 2: Add testcases using raw JSON to avoid serialization issues
            String testcase1Json = "{\"input\":\"[2,7,11,15]\\n9\",\"expectedOutput\":\"[0,1]\",\"isHidden\":false}";
            mockMvc.perform(post("/api/problems/" + problemId + "/testcases")
                    .header("Authorization", "Bearer " + problemSetterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(testcase1Json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("[2,7,11,15]\n9"))
                    .andExpect(jsonPath("$.expectedOutput").value("[0,1]"));

            String testcase2Json = "{\"input\":\"[3,2,4]\\n6\",\"expectedOutput\":\"[1,2]\",\"isHidden\":true}";
            mockMvc.perform(post("/api/problems/" + problemId + "/testcases")
                    .header("Authorization", "Bearer " + problemSetterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(testcase2Json))
                    .andExpect(status().isOk());

            // Step 3: Request review
            mockMvc.perform(post("/api/problems/" + problemId + "/request-review")
                    .header("Authorization", "Bearer " + problemSetterToken))
                    .andExpect(status().isNoContent());

            // Step 4: Admin publishes the problem
            mockMvc.perform(post("/api/admin/problems/" + problemId + "/publish")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Step 5: Verify problem is now visible to regular users
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Two Sum"));
        }

        @Test
        @DisplayName("should allow admin to reject problem")
        void shouldAllowAdminToRejectProblem() throws Exception {
            Long problemId = createAndSubmitProblem();

            // Admin rejects the problem
            mockMvc.perform(post("/api/admin/problems/" + problemId + "/reject")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should allow admin to archive published problem")
        void shouldAllowAdminToArchiveProblem() throws Exception {
            Long problemId = createAndPublishProblem();

            // Admin archives the problem
            mockMvc.perform(post("/api/admin/problems/" + problemId + "/archive")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Verify problem is no longer visible in listing
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("Access Control at Each Stage")
    class AccessControlAtEachStage {

        @Test
        @DisplayName("should deny regular user from creating problems")
        void shouldDenyUserFromCreatingProblems() throws Exception {
            CreateProblemRequest createRequest = new CreateProblemRequest(
                "User Problem",
                "This should fail",
                "None",
                Difficulty.EASY,
                List.of("test"),
                null
            );

            mockMvc.perform(post("/api/problems")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should deny non-owner from editing problem")
        void shouldDenyNonOwnerFromEditingProblem() throws Exception {
            Long problemId = createDraftProblem();

            // Create another problem setter
            String otherSetterToken = createUserAndGetToken(
                "Other Setter", 
                "other@example.com", 
                "Password123!", 
                Role.PROBLEM_SETTER
            );

            // Try to edit as different problem setter
            mockMvc.perform(put("/api/problems/" + problemId)
                    .header("Authorization", "Bearer " + otherSetterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Hacked Title\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should allow admin to edit any problem")
        void shouldAllowAdminToEditAnyProblem() throws Exception {
            Long problemId = createDraftProblem();

            mockMvc.perform(put("/api/problems/" + problemId)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Admin Edited Title\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Admin Edited Title"));
        }

        @Test
        @DisplayName("should deny problem setter from publishing problems")
        void shouldDenyProblemSetterFromPublishing() throws Exception {
            Long problemId = createAndSubmitProblem();

            mockMvc.perform(post("/api/admin/problems/" + problemId + "/publish")
                    .header("Authorization", "Bearer " + problemSetterToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should deny regular user from viewing pending problems")
        void shouldDenyUserFromViewingPendingProblems() throws Exception {
            mockMvc.perform(get("/api/admin/problems/pending")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should allow admin to view pending problems")
        void shouldAllowAdminToViewPendingProblems() throws Exception {
            createAndSubmitProblem();

            mockMvc.perform(get("/api/admin/problems/pending")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("Testcase Access Control")
    class TestcaseAccessControl {

        @Test
        @DisplayName("should hide hidden testcases from regular users")
        void shouldHideHiddenTestcasesFromUsers() throws Exception {
            Long problemId = createAndPublishProblem();

            // Regular user should only see visible testcases
            mockMvc.perform(get("/api/problems/" + problemId + "/testcases")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.hidden == true)]").doesNotExist());
        }

        @Test
        @DisplayName("should show all testcases to admin")
        void shouldShowAllTestcasesToAdmin() throws Exception {
            Long problemId = createAndPublishProblem();

            // Admin should see all testcases including hidden ones
            mockMvc.perform(get("/api/problems/" + problemId + "/testcases")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    // Helper methods
    private Long createDraftProblem() throws Exception {
        CreateProblemRequest createRequest = new CreateProblemRequest(
            "Test Problem",
            "Test statement",
            "None",
            Difficulty.EASY,
            List.of("test"),
            null
        );

        MvcResult result = mockMvc.perform(post("/api/problems")
                .header("Authorization", "Bearer " + problemSetterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createAndSubmitProblem() throws Exception {
        Long problemId = createDraftProblem();

        // Add required testcases using raw JSON
        String visibleTestcase = "{\"input\":\"input\",\"expectedOutput\":\"output\",\"isHidden\":false}";
        mockMvc.perform(post("/api/problems/" + problemId + "/testcases")
                .header("Authorization", "Bearer " + problemSetterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(visibleTestcase))
                .andExpect(status().isOk());

        String hiddenTestcase = "{\"input\":\"hidden input\",\"expectedOutput\":\"hidden output\",\"isHidden\":true}";
        mockMvc.perform(post("/api/problems/" + problemId + "/testcases")
                .header("Authorization", "Bearer " + problemSetterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hiddenTestcase))
                .andExpect(status().isOk());

        // Request review
        mockMvc.perform(post("/api/problems/" + problemId + "/request-review")
                .header("Authorization", "Bearer " + problemSetterToken))
                .andExpect(status().isNoContent());

        return problemId;
    }

    private Long createAndPublishProblem() throws Exception {
        Long problemId = createAndSubmitProblem();

        // Publish
        mockMvc.perform(post("/api/admin/problems/" + problemId + "/publish")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        return problemId;
    }
}
