package com.codearena.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.codearena.config.SecurityConfig;
import com.codearena.dto.PlatformStatsDto;
import com.codearena.dto.ProblemManagementDto;
import com.codearena.dto.SubmissionDto;
import com.codearena.dto.UserListDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.security.JwtService;
import com.codearena.service.AdminAuditService;
import com.codearena.service.AdminService;
import com.codearena.service.ProblemService;
import com.codearena.service.SubmissionService;
import com.codearena.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for AdminController endpoints.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Platform statistics endpoint</li>
 *   <li>User listing with filters</li>
 *   <li>Problem listing with filters</li>
 *   <li>Bulk operations</li>
 *   <li>Submission monitoring with filters</li>
 *   <li>Role-based access control</li>
 * </ul>
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private ProblemService problemService;

    @MockBean
    private UserService userService;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private AdminAuditService adminAuditService;

    @MockBean
    private JwtService jwtService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("Admin", "admin@test.com", "hashedPassword", Role.ADMIN);
        adminUser.setId(1L);
    }

    @Nested
    @DisplayName("GET /api/admin/stats")
    class GetPlatformStats {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return platform statistics for admin")
        void shouldReturnPlatformStats() throws Exception {
            // Given
            Map<Role, Long> usersByRole = new EnumMap<>(Role.class);
            usersByRole.put(Role.USER, 100L);
            usersByRole.put(Role.PROBLEM_SETTER, 10L);
            usersByRole.put(Role.ADMIN, 2L);

            Map<ProblemStatus, Long> problemsByStatus = new EnumMap<>(ProblemStatus.class);
            problemsByStatus.put(ProblemStatus.PUBLISHED, 50L);
            problemsByStatus.put(ProblemStatus.PENDING_REVIEW, 5L);
            problemsByStatus.put(ProblemStatus.DRAFT, 10L);
            problemsByStatus.put(ProblemStatus.REJECTED, 2L);
            problemsByStatus.put(ProblemStatus.ARCHIVED, 3L);

            Map<Difficulty, Long> problemsByDifficulty = new EnumMap<>(Difficulty.class);
            problemsByDifficulty.put(Difficulty.EASY, 20L);
            problemsByDifficulty.put(Difficulty.MEDIUM, 30L);
            problemsByDifficulty.put(Difficulty.HARD, 20L);

            PlatformStatsDto stats = new PlatformStatsDto(
                112L, usersByRole, 70L, problemsByStatus, problemsByDifficulty,
                1500L, 45L, 65.5
            );

            when(adminService.getPlatformStats()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(112))
                .andExpect(jsonPath("$.totalProblems").value(70))
                .andExpect(jsonPath("$.totalSubmissions").value(1500))
                .andExpect(jsonPath("$.submissionsToday").value(45))
                .andExpect(jsonPath("$.acceptanceRate").value(65.5))
                .andExpect(jsonPath("$.usersByRole.USER").value(100))
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(2))
                .andExpect(jsonPath("$.problemsByStatus.PUBLISHED").value(50));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 for non-admin users")
        void shouldReturn403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return paginated user list")
        void shouldReturnPaginatedUsers() throws Exception {
            // Given
            UserListDto user1 = new UserListDto(1L, "User One", "user1@test.com", Role.USER, LocalDateTime.now(), 5);
            UserListDto user2 = new UserListDto(2L, "User Two", "user2@test.com", Role.PROBLEM_SETTER, LocalDateTime.now(), 10);

            PageImpl<UserListDto> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 10), 2);
            when(adminService.listUsers(any(), any(), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/users")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("User One"))
                .andExpect(jsonPath("$.content[1].role").value("PROBLEM_SETTER"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter users by role")
        void shouldFilterUsersByRole() throws Exception {
            // Given
            UserListDto user = new UserListDto(1L, "Admin User", "admin@test.com", Role.ADMIN, LocalDateTime.now(), 0);
            PageImpl<UserListDto> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
            when(adminService.listUsers(eq(Role.ADMIN), any(), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/users")
                    .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter users by search term")
        void shouldFilterUsersBySearch() throws Exception {
            // Given
            UserListDto user = new UserListDto(1L, "John Doe", "john@test.com", Role.USER, LocalDateTime.now(), 3);
            PageImpl<UserListDto> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
            when(adminService.listUsers(any(), eq("john"), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/users")
                    .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/problems")
    class GetAllProblems {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return paginated problem list")
        void shouldReturnPaginatedProblems() throws Exception {
            // Given
            ProblemManagementDto problem = new ProblemManagementDto(
                1L, "Two Sum", "two-sum", Difficulty.EASY, ProblemStatus.PUBLISHED,
                1L, "John Doe", 5, 100L, LocalDateTime.now()
            );

            PageImpl<ProblemManagementDto> page = new PageImpl<>(List.of(problem), PageRequest.of(0, 10), 1);
            when(adminService.listAllProblems(any(), any(), any(), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Two Sum"))
                .andExpect(jsonPath("$.content[0].authorName").value("John Doe"))
                .andExpect(jsonPath("$.content[0].testcaseCount").value(5));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter problems by status and difficulty")
        void shouldFilterProblemsByStatusAndDifficulty() throws Exception {
            // Given
            ProblemManagementDto problem = new ProblemManagementDto(
                1L, "Hard Problem", "hard-problem", Difficulty.HARD, ProblemStatus.PENDING_REVIEW,
                2L, "Jane Smith", 10, 0L, LocalDateTime.now()
            );

            PageImpl<ProblemManagementDto> page = new PageImpl<>(List.of(problem), PageRequest.of(0, 10), 1);
            when(adminService.listAllProblems(eq(ProblemStatus.PENDING_REVIEW), eq(Difficulty.HARD), any(), any()))
                .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/problems")
                    .param("status", "PENDING_REVIEW")
                    .param("difficulty", "HARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.content[0].difficulty").value("HARD"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/problems/bulk/*")
    class BulkOperations {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should bulk publish problems")
        void shouldBulkPublishProblems() throws Exception {
            // Given
            when(adminService.bulkPublish(any(), any())).thenReturn(3);

            // When & Then
            mockMvc.perform(post("/api/admin/problems/bulk/publish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": [1, 2, 3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.action").value("published"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should bulk reject problems")
        void shouldBulkRejectProblems() throws Exception {
            // Given
            when(adminService.bulkReject(any(), any())).thenReturn(2);

            // When & Then
            mockMvc.perform(post("/api/admin/problems/bulk/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": [1, 2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.action").value("rejected"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should bulk archive problems")
        void shouldBulkArchiveProblems() throws Exception {
            // Given
            when(adminService.bulkArchive(any(), any())).thenReturn(5);

            // When & Then
            mockMvc.perform(post("/api/admin/problems/bulk/archive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": [1, 2, 3, 4, 5]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.action").value("archived"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should reject empty bulk request")
        void shouldRejectEmptyBulkRequest() throws Exception {
            mockMvc.perform(post("/api/admin/problems/bulk/publish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": []}"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/submissions")
    class GetSubmissions {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return paginated submissions")
        void shouldReturnPaginatedSubmissions() throws Exception {
            // Given
            SubmissionDto submission = new SubmissionDto(
                "sub-1", 1L, 1L, "Two Sum", 71, "print('hello')",
                Verdict.ACCEPTED, 0.05, 8192, 5, 5, LocalDateTime.now()
            );

            PageImpl<SubmissionDto> page = new PageImpl<>(List.of(submission), PageRequest.of(0, 10), 1);
            when(submissionService.getAllSubmissionsFiltered(any(), any(), any(), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].problemTitle").value("Two Sum"))
                .andExpect(jsonPath("$.content[0].verdict").value("ACCEPTED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter submissions by verdict")
        void shouldFilterSubmissionsByVerdict() throws Exception {
            // Given
            SubmissionDto submission = new SubmissionDto(
                "sub-2", 1L, 2L, "Problem", 71, "code",
                Verdict.WRONG_ANSWER, 0.1, 8192, 3, 5, LocalDateTime.now()
            );

            PageImpl<SubmissionDto> page = new PageImpl<>(List.of(submission), PageRequest.of(0, 10), 1);
            when(submissionService.getAllSubmissionsFiltered(eq(Verdict.WRONG_ANSWER), any(), any(), any()))
                .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/submissions")
                    .param("verdict", "WRONG_ANSWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].verdict").value("WRONG_ANSWER"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter submissions by userId")
        void shouldFilterSubmissionsByUserId() throws Exception {
            // Given
            SubmissionDto submission = new SubmissionDto(
                "sub-3", 5L, 1L, "Problem", 71, "code",
                Verdict.ACCEPTED, 0.05, 8192, 5, 5, LocalDateTime.now()
            );

            PageImpl<SubmissionDto> page = new PageImpl<>(List.of(submission), PageRequest.of(0, 10), 1);
            when(submissionService.getAllSubmissionsFiltered(any(), eq(5L), any(), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/admin/submissions")
                    .param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(5));
        }
    }

    @Nested
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("regular users should not access admin endpoints")
        void regularUsersShouldNotAccessAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/admin/problems")).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/admin/submissions")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PROBLEM_SETTER")
        @DisplayName("problem setters should not access admin endpoints")
        void problemSettersShouldNotAccessAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/problems/bulk/publish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": [1]}"))
                .andExpect(status().isForbidden());
        }
    }
}
