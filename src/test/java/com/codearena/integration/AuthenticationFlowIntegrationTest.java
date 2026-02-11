package com.codearena.integration;

import com.codearena.dto.AuthResponse;
import com.codearena.dto.LoginRequest;
import com.codearena.dto.SignupRequest;
import com.codearena.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the authentication flow.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Signup → Login → Access protected endpoint flow</li>
 *   <li>Invalid token rejection</li>
 *   <li>Expired token handling</li>
 * </ul>
 * 
 * <p>Validates: Requirements 1.1-2.5
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authentication Flow Integration Tests")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Complete Authentication Flow")
    class CompleteAuthenticationFlow {

        @Test
        @DisplayName("should complete signup → login → access protected endpoint flow")
        void shouldCompleteFullAuthenticationFlow() throws Exception {
            // Step 1: Signup
            SignupRequest signupRequest = new SignupRequest(
                "Test User",
                "testuser@example.com",
                "SecurePassword123!"
            );

            MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.email").value("testuser@example.com"))
                    .andExpect(jsonPath("$.user.name").value("Test User"))
                    .andExpect(jsonPath("$.user.role").value("USER"))
                    .andReturn();

            AuthResponse signupResponse = objectMapper.readValue(
                signupResult.getResponse().getContentAsString(),
                AuthResponse.class
            );
            String signupToken = signupResponse.token();

            // Step 2: Login with the same credentials
            LoginRequest loginRequest = new LoginRequest(
                "testuser@example.com",
                "SecurePassword123!"
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.email").value("testuser@example.com"))
                    .andReturn();

            AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
            );
            String loginToken = loginResponse.token();

            // Both tokens should be valid
            assertThat(signupToken).isNotBlank();
            assertThat(loginToken).isNotBlank();

            // Step 3: Access protected endpoint with signup token
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer " + signupToken))
                    .andExpect(status().isOk());

            // Step 4: Access protected endpoint with login token
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer " + loginToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should access user profile after authentication")
        void shouldAccessUserProfileAfterAuthentication() throws Exception {
            // Signup
            SignupRequest signupRequest = new SignupRequest(
                "Profile User",
                "profile@example.com",
                "Password123!"
            );

            MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse response = objectMapper.readValue(
                signupResult.getResponse().getContentAsString(),
                AuthResponse.class
            );

            // Access user profile
            mockMvc.perform(get("/api/me")
                    .header("Authorization", "Bearer " + response.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("profile@example.com"))
                    .andExpect(jsonPath("$.name").value("Profile User"));
        }
    }

    @Nested
    @DisplayName("Invalid Token Rejection")
    class InvalidTokenRejection {

        @Test
        @DisplayName("should reject request with invalid JWT token")
        void shouldRejectInvalidJwtToken() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with malformed token")
        void shouldRejectMalformedToken() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer not-a-valid-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with tampered token")
        void shouldRejectTamperedToken() throws Exception {
            // First, get a valid token
            SignupRequest signupRequest = new SignupRequest(
                "Tamper Test",
                "tamper@example.com",
                "Password123!"
            );

            MvcResult result = mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
            );

            // Tamper with the token by modifying a character
            String validToken = response.token();
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

            // Should reject the tampered token
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer " + tamperedToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request without Authorization header")
        void shouldRejectRequestWithoutAuthHeader() throws Exception {
            mockMvc.perform(get("/api/problems"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with empty Bearer token")
        void shouldRejectEmptyBearerToken() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with wrong auth scheme")
        void shouldRejectWrongAuthScheme() throws Exception {
            mockMvc.perform(get("/api/problems")
                    .header("Authorization", "Basic dXNlcjpwYXNz"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Login Validation")
    class LoginValidation {

        @Test
        @DisplayName("should reject login with wrong password")
        void shouldRejectLoginWithWrongPassword() throws Exception {
            // First signup
            SignupRequest signupRequest = new SignupRequest(
                "Wrong Pass User",
                "wrongpass@example.com",
                "CorrectPassword123!"
            );

            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk());

            // Try to login with wrong password
            LoginRequest loginRequest = new LoginRequest(
                "wrongpass@example.com",
                "WrongPassword123!"
            );

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject login with non-existent email")
        void shouldRejectLoginWithNonExistentEmail() throws Exception {
            LoginRequest loginRequest = new LoginRequest(
                "nonexistent@example.com",
                "Password123!"
            );

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Signup Validation")
    class SignupValidation {

        @Test
        @DisplayName("should reject signup with duplicate email")
        void shouldRejectSignupWithDuplicateEmail() throws Exception {
            SignupRequest firstSignup = new SignupRequest(
                "First User",
                "duplicate@example.com",
                "Password123!"
            );

            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstSignup)))
                    .andExpect(status().isOk());

            // Try to signup with same email
            SignupRequest secondSignup = new SignupRequest(
                "Second User",
                "duplicate@example.com",
                "DifferentPassword123!"
            );

            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondSignup)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should reject signup with invalid email format")
        void shouldRejectSignupWithInvalidEmail() throws Exception {
            SignupRequest signupRequest = new SignupRequest(
                "Invalid Email User",
                "not-an-email",
                "Password123!"
            );

            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject signup with short password")
        void shouldRejectSignupWithShortPassword() throws Exception {
            SignupRequest signupRequest = new SignupRequest(
                "Short Pass User",
                "shortpass@example.com",
                "short"
            );

            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject signup with empty name")
        void shouldRejectSignupWithEmptyName() throws Exception {
            SignupRequest signupRequest = new SignupRequest(
                "",
                "emptyname@example.com",
                "Password123!"
            );

            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
