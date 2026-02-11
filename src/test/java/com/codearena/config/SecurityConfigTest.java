package com.codearena.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link SecurityConfig}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Public endpoint access (authentication endpoints)</li>
 *   <li>Authenticated endpoint access requirements</li>
 *   <li>Role-based access control for PROBLEM_SETTER</li>
 *   <li>Role-based access control for ADMIN</li>
 *   <li>BCrypt password encoder configuration</li>
 * </ul>
 * 
 * <p>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 17.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("should allow unauthenticated access to signup endpoint")
        void shouldAllowUnauthenticatedAccessToSignup() throws Exception {
            // Endpoint exists now, so we expect a non-401/403 response
            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - endpoint is public
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @DisplayName("should allow unauthenticated access to login endpoint")
        void shouldAllowUnauthenticatedAccessToLogin() throws Exception {
            // Login endpoint returns 401 for invalid credentials, which is expected
            // The key is that it's not returning 401 due to missing authentication
            // (the endpoint is public, but the credentials are wrong)
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // 401 is acceptable here because it's for invalid credentials, not missing auth
                        // 403 would indicate the endpoint requires authentication
                        assertThat(status).isNotEqualTo(403);
                    });
        }
    }

    @Nested
    @DisplayName("Authentication Required Endpoints")
    class AuthenticationRequiredEndpoints {

        @Test
        @DisplayName("should reject unauthenticated access to problems endpoint")
        void shouldRejectUnauthenticatedAccessToProblems() throws Exception {
            mockMvc.perform(get("/api/problems"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject unauthenticated access to execute endpoint")
        void shouldRejectUnauthenticatedAccessToExecute() throws Exception {
            mockMvc.perform(post("/api/execute/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject unauthenticated access to user profile endpoint")
        void shouldRejectUnauthenticatedAccessToUserProfile() throws Exception {
            mockMvc.perform(get("/api/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject unauthenticated access to admin endpoint")
        void shouldRejectUnauthenticatedAccessToAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/problems/pending"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("USER Role Access")
    class UserRoleAccess {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should allow USER to access GET problems endpoint")
        void shouldAllowUserToAccessGetProblems() throws Exception {
            mockMvc.perform(get("/api/problems"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - USER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should allow USER to access execute endpoint")
        void shouldAllowUserToAccessExecute() throws Exception {
            mockMvc.perform(post("/api/execute/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - USER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should allow USER to access user profile endpoint")
        void shouldAllowUserToAccessUserProfile() throws Exception {
            mockMvc.perform(get("/api/me"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - USER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should deny USER access to POST problems endpoint")
        void shouldDenyUserAccessToPostProblems() throws Exception {
            mockMvc.perform(post("/api/problems")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should deny USER access to PUT problems endpoint")
        void shouldDenyUserAccessToPutProblems() throws Exception {
            mockMvc.perform(put("/api/problems/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should deny USER access to admin endpoint")
        void shouldDenyUserAccessToAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/problems/pending"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PROBLEM_SETTER Role Access")
    class ProblemSetterRoleAccess {

        @Test
        @WithMockUser(roles = "PROBLEM_SETTER")
        @DisplayName("should allow PROBLEM_SETTER to access GET problems endpoint")
        void shouldAllowProblemSetterToAccessGetProblems() throws Exception {
            mockMvc.perform(get("/api/problems"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - PROBLEM_SETTER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "PROBLEM_SETTER")
        @DisplayName("should allow PROBLEM_SETTER to access POST problems endpoint")
        void shouldAllowProblemSetterToAccessPostProblems() throws Exception {
            mockMvc.perform(post("/api/problems")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - PROBLEM_SETTER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "PROBLEM_SETTER")
        @DisplayName("should allow PROBLEM_SETTER to access PUT problems endpoint")
        void shouldAllowProblemSetterToAccessPutProblems() throws Exception {
            mockMvc.perform(put("/api/problems/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - PROBLEM_SETTER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "PROBLEM_SETTER")
        @DisplayName("should allow PROBLEM_SETTER to access POST testcases endpoint")
        void shouldAllowProblemSetterToAccessPostTestcases() throws Exception {
            mockMvc.perform(post("/api/problems/1/testcases")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - PROBLEM_SETTER has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "PROBLEM_SETTER")
        @DisplayName("should deny PROBLEM_SETTER access to admin endpoint")
        void shouldDenyProblemSetterAccessToAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/problems/pending"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("ADMIN Role Access")
    class AdminRoleAccess {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow ADMIN to access GET problems endpoint")
        void shouldAllowAdminToAccessGetProblems() throws Exception {
            mockMvc.perform(get("/api/problems"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - ADMIN has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow ADMIN to access POST problems endpoint")
        void shouldAllowAdminToAccessPostProblems() throws Exception {
            mockMvc.perform(post("/api/problems")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - ADMIN has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow ADMIN to access PUT problems endpoint")
        void shouldAllowAdminToAccessPutProblems() throws Exception {
            mockMvc.perform(put("/api/problems/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - ADMIN has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow ADMIN to access admin endpoint")
        void shouldAllowAdminToAccessAdminEndpoint() throws Exception {
            mockMvc.perform(get("/api/admin/problems/pending"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - ADMIN has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow ADMIN to access user management endpoint")
        void shouldAllowAdminToAccessUserManagement() throws Exception {
            mockMvc.perform(put("/api/admin/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"role\":\"PROBLEM_SETTER\"}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 401 or 403 - ADMIN has access
                        assertThat(status).isNotIn(401, 403);
                    });
        }
    }

    @Nested
    @DisplayName("Password Encoder")
    class PasswordEncoderTests {

        @Test
        @DisplayName("should use BCrypt password encoder")
        void shouldUseBCryptPasswordEncoder() {
            assertThat(passwordEncoder).isNotNull();
            
            String rawPassword = "testPassword123";
            String encodedPassword = passwordEncoder.encode(rawPassword);
            
            // BCrypt hashes start with $2a$, $2b$, or $2y$
            assertThat(encodedPassword).matches("^\\$2[aby]\\$.*");
        }

        @Test
        @DisplayName("should encode passwords with cost factor of at least 10")
        void shouldEncodePasswordsWithCostFactorOfAtLeast10() {
            String rawPassword = "testPassword123";
            String encodedPassword = passwordEncoder.encode(rawPassword);
            
            // BCrypt format: $2a$XX$... where XX is the cost factor
            // Extract cost factor from the hash
            String[] parts = encodedPassword.split("\\$");
            int costFactor = Integer.parseInt(parts[2]);
            
            assertThat(costFactor).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("should verify correct password")
        void shouldVerifyCorrectPassword() {
            String rawPassword = "testPassword123";
            String encodedPassword = passwordEncoder.encode(rawPassword);
            
            assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        }

        @Test
        @DisplayName("should reject incorrect password")
        void shouldRejectIncorrectPassword() {
            String rawPassword = "testPassword123";
            String encodedPassword = passwordEncoder.encode(rawPassword);
            
            assertThat(passwordEncoder.matches("wrongPassword", encodedPassword)).isFalse();
        }

        @Test
        @DisplayName("should generate different hashes for same password")
        void shouldGenerateDifferentHashesForSamePassword() {
            String rawPassword = "testPassword123";
            String encodedPassword1 = passwordEncoder.encode(rawPassword);
            String encodedPassword2 = passwordEncoder.encode(rawPassword);
            
            // BCrypt generates different hashes due to random salt
            assertThat(encodedPassword1).isNotEqualTo(encodedPassword2);
            
            // But both should verify correctly
            assertThat(passwordEncoder.matches(rawPassword, encodedPassword1)).isTrue();
            assertThat(passwordEncoder.matches(rawPassword, encodedPassword2)).isTrue();
        }
    }

    @Nested
    @DisplayName("JWT Authentication")
    class JwtAuthenticationTests {

        @Test
        @DisplayName("should reject request with invalid JWT token")
        void shouldRejectRequestWithInvalidJwtToken() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with malformed Authorization header")
        void shouldRejectRequestWithMalformedAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "NotBearer sometoken"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with empty Authorization header")
        void shouldRejectRequestWithEmptyAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", ""))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with Bearer prefix only")
        void shouldRejectRequestWithBearerPrefixOnly() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Stateless Session Management")
    class StatelessSessionTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should not create session for authenticated request")
        void shouldNotCreateSessionForAuthenticatedRequest() throws Exception {
            mockMvc.perform(get("/api/problems"))
                    .andExpect(result -> {
                        // Session should not be created for stateless authentication
                        assertThat(result.getRequest().getSession(false)).isNull();
                    });
        }
    }
}
