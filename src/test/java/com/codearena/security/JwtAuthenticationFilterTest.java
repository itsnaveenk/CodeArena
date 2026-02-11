package com.codearena.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Successful authentication with valid JWT token</li>
 *   <li>Skipping authentication when no Authorization header is present</li>
 *   <li>Skipping authentication when Authorization header doesn't have Bearer prefix</li>
 *   <li>Handling invalid/expired tokens gracefully</li>
 *   <li>Handling user not found in database</li>
 *   <li>Skipping authentication when already authenticated</li>
 * </ul>
 * 
 * <p>Validates: Requirements 2.3, 2.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    private User createTestUser(Long id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private Claims createClaims(String email) {
        return Jwts.claims()
                .add("email", email)
                .add("userId", 1L)
                .add("role", "USER")
                .build();
    }

    @Nested
    @DisplayName("Successful Authentication")
    class SuccessfulAuthentication {

        @Test
        @DisplayName("should authenticate user with valid Bearer token")
        void shouldAuthenticateUserWithValidBearerToken() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "test@example.com";
            User user = createTestUser(1L, "Test User", email, Role.USER);
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(user);
            assertThat(authentication.getAuthorities()).hasSize(1);
            assertThat(authentication.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("should authenticate PROBLEM_SETTER with valid token")
        void shouldAuthenticateProblemSetterWithValidToken() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "setter@example.com";
            User user = createTestUser(2L, "Problem Setter", email, Role.PROBLEM_SETTER);
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_PROBLEM_SETTER");
        }

        @Test
        @DisplayName("should authenticate ADMIN with valid token")
        void shouldAuthenticateAdminWithValidToken() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "admin@example.com";
            User user = createTestUser(3L, "Admin User", email, Role.ADMIN);
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should continue filter chain after successful authentication")
        void shouldContinueFilterChainAfterSuccessfulAuthentication() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "test@example.com";
            User user = createTestUser(1L, "Test User", email, Role.USER);
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("No Authorization Header")
    class NoAuthorizationHeader {

        @Test
        @DisplayName("should not authenticate when Authorization header is missing")
        void shouldNotAuthenticateWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
            // Given - no Authorization header

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should continue filter chain when Authorization header is missing")
        void shouldContinueFilterChainWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
            // Given - no Authorization header

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Invalid Authorization Header Format")
    class InvalidAuthorizationHeaderFormat {

        @Test
        @DisplayName("should not authenticate when Authorization header doesn't start with Bearer")
        void shouldNotAuthenticateWhenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
            // Given
            request.addHeader("Authorization", "Basic sometoken");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should not authenticate when Bearer token is empty")
        void shouldNotAuthenticateWhenBearerTokenIsEmpty() throws ServletException, IOException {
            // Given
            request.addHeader("Authorization", "Bearer ");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should not authenticate when Bearer token is whitespace only")
        void shouldNotAuthenticateWhenBearerTokenIsWhitespaceOnly() throws ServletException, IOException {
            // Given
            request.addHeader("Authorization", "Bearer    ");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should continue filter chain with invalid header format")
        void shouldContinueFilterChainWithInvalidHeaderFormat() throws ServletException, IOException {
            // Given
            request.addHeader("Authorization", "InvalidFormat");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Invalid Token")
    class InvalidToken {

        @Test
        @DisplayName("should not authenticate when token is invalid")
        void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
            // Given
            String token = "invalid.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).validateAndExtractClaims(anyString());
        }

        @Test
        @DisplayName("should continue filter chain when token is invalid")
        void shouldContinueFilterChainWhenTokenIsInvalid() throws ServletException, IOException {
            // Given
            String token = "invalid.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("should handle exception during token validation gracefully")
        void shouldHandleExceptionDuringTokenValidationGracefully() throws ServletException, IOException {
            // Given
            String token = "problematic.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenThrow(new RuntimeException("Unexpected error"));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("User Not Found")
    class UserNotFound {

        @Test
        @DisplayName("should not authenticate when user is not found in database")
        void shouldNotAuthenticateWhenUserNotFound() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "nonexistent@example.com";
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should continue filter chain when user is not found")
        void shouldContinueFilterChainWhenUserNotFound() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "nonexistent@example.com";
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Missing Email Claim")
    class MissingEmailClaim {

        @Test
        @DisplayName("should not authenticate when email claim is missing")
        void shouldNotAuthenticateWhenEmailClaimIsMissing() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            Claims claims = Jwts.claims()
                    .add("userId", 1L)
                    .add("role", "USER")
                    .build();
            // No email claim

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("should not authenticate when email claim is empty")
        void shouldNotAuthenticateWhenEmailClaimIsEmpty() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            Claims claims = Jwts.claims()
                    .add("email", "")
                    .add("userId", 1L)
                    .add("role", "USER")
                    .build();

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("should not authenticate when email claim is whitespace only")
        void shouldNotAuthenticateWhenEmailClaimIsWhitespaceOnly() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            Claims claims = Jwts.claims()
                    .add("email", "   ")
                    .add("userId", 1L)
                    .add("role", "USER")
                    .build();

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(userRepository, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("Already Authenticated")
    class AlreadyAuthenticated {

        @Test
        @DisplayName("should skip authentication when SecurityContext already has authentication")
        void shouldSkipAuthenticationWhenAlreadyAuthenticated() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);

            // Set up existing authentication
            Authentication existingAuth = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService, never()).isTokenValid(anyString());
            verify(jwtService, never()).validateAndExtractClaims(anyString());
            verify(userRepository, never()).findByEmail(anyString());
            
            // Existing authentication should remain unchanged
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
        }
    }

    @Nested
    @DisplayName("Token Extraction Edge Cases")
    class TokenExtractionEdgeCases {

        @Test
        @DisplayName("should handle token with extra spaces after Bearer prefix")
        void shouldHandleTokenWithExtraSpacesAfterBearerPrefix() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "test@example.com";
            User user = createTestUser(1L, "Test User", email, Role.USER);
            Claims claims = createClaims(email);

            // Token with extra spaces - should be trimmed
            request.addHeader("Authorization", "Bearer   " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
        }

        @Test
        @DisplayName("should be case-sensitive for Bearer prefix")
        void shouldBeCaseSensitiveForBearerPrefix() throws ServletException, IOException {
            // Given - lowercase "bearer" instead of "Bearer"
            request.addHeader("Authorization", "bearer valid.jwt.token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should handle empty Authorization header")
        void shouldHandleEmptyAuthorizationHeader() throws ServletException, IOException {
            // Given
            request.addHeader("Authorization", "");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).isTokenValid(anyString());
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandling {

        @Test
        @DisplayName("should handle exception during claims extraction gracefully")
        void shouldHandleExceptionDuringClaimsExtractionGracefully() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenThrow(new RuntimeException("Claims extraction failed"));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("should handle exception during user lookup gracefully")
        void shouldHandleExceptionDuringUserLookupGracefully() throws ServletException, IOException {
            // Given
            String token = "valid.jwt.token";
            String email = "test@example.com";
            Claims claims = createClaims(email);

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.isTokenValid(token)).thenReturn(true);
            when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
            when(userRepository.findByEmail(email)).thenThrow(new RuntimeException("Database error"));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }
}
