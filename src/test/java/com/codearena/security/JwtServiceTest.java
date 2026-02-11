package com.codearena.security;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.codearena.config.JwtConfig;
import com.codearena.entity.Role;
import com.codearena.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

/**
 * Unit tests for {@link JwtServiceImpl}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Token generation with correct claims (userId, email, role)</li>
 *   <li>Token validation and claim extraction</li>
 *   <li>Expired token rejection</li>
 *   <li>Invalid/malformed token rejection</li>
 *   <li>Tampered token rejection</li>
 * </ul>
 * 
 * <p>Validates: Requirements 2.1, 2.3, 2.4, 2.5
 */
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "this-is-a-test-secret-key-that-is-at-least-32-characters-long";
    private static final long TEST_EXPIRATION = 86400000L; // 24 hours

    private JwtService jwtService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtConfig.setExpiration(TEST_EXPIRATION);
        jwtService = new JwtServiceImpl(jwtConfig);
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

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should generate valid token for USER role")
        void shouldGenerateValidTokenForUserRole() {
            // Given
            User user = createTestUser(1L, "John Doe", "john@example.com", Role.USER);

            // When
            String token = jwtService.generateToken(user);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should generate valid token for PROBLEM_SETTER role")
        void shouldGenerateValidTokenForProblemSetterRole() {
            // Given
            User user = createTestUser(2L, "Jane Setter", "jane@example.com", Role.PROBLEM_SETTER);

            // When
            String token = jwtService.generateToken(user);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should generate valid token for ADMIN role")
        void shouldGenerateValidTokenForAdminRole() {
            // Given
            User user = createTestUser(3L, "Admin User", "admin@example.com", Role.ADMIN);

            // When
            String token = jwtService.generateToken(user);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            assertThatThrownBy(() -> jwtService.generateToken(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User cannot be null");
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Given
            User user1 = createTestUser(1L, "User One", "user1@example.com", Role.USER);
            User user2 = createTestUser(2L, "User Two", "user2@example.com", Role.USER);

            // When
            String token1 = jwtService.generateToken(user1);
            String token2 = jwtService.generateToken(user2);

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("validateAndExtractClaims")
    class ValidateAndExtractClaims {

        @Test
        @DisplayName("should extract userId claim correctly")
        void shouldExtractUserIdClaimCorrectly() {
            // Given
            User user = createTestUser(42L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
        }

        @Test
        @DisplayName("should extract email claim correctly")
        void shouldExtractEmailClaimCorrectly() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should extract role claim correctly for USER")
        void shouldExtractRoleClaimCorrectlyForUser() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.get("role", String.class)).isEqualTo("USER");
        }

        @Test
        @DisplayName("should extract role claim correctly for PROBLEM_SETTER")
        void shouldExtractRoleClaimCorrectlyForProblemSetter() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.PROBLEM_SETTER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.get("role", String.class)).isEqualTo("PROBLEM_SETTER");
        }

        @Test
        @DisplayName("should extract role claim correctly for ADMIN")
        void shouldExtractRoleClaimCorrectlyForAdmin() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.ADMIN);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should extract subject claim as email")
        void shouldExtractSubjectClaimAsEmail() {
            // Given
            User user = createTestUser(1L, "Test User", "subject@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.getSubject()).isEqualTo("subject@example.com");
        }

        @Test
        @DisplayName("should have issuedAt claim")
        void shouldHaveIssuedAtClaim() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.getIssuedAt()).isNotNull();
        }

        @Test
        @DisplayName("should have expiration claim")
        void shouldHaveExpirationClaim() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("should throw exception for null token")
        void shouldThrowExceptionForNullToken() {
            assertThatThrownBy(() -> jwtService.validateAndExtractClaims(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token cannot be null or empty");
        }

        @Test
        @DisplayName("should throw exception for empty token")
        void shouldThrowExceptionForEmptyToken() {
            assertThatThrownBy(() -> jwtService.validateAndExtractClaims(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token cannot be null or empty");
        }

        @Test
        @DisplayName("should throw exception for whitespace-only token")
        void shouldThrowExceptionForWhitespaceOnlyToken() {
            assertThatThrownBy(() -> jwtService.validateAndExtractClaims("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token cannot be null or empty");
        }

        @Test
        @DisplayName("should throw exception for malformed token")
        void shouldThrowExceptionForMalformedToken() {
            assertThatThrownBy(() -> jwtService.validateAndExtractClaims("not.a.valid.jwt.token"))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("should throw exception for completely invalid token")
        void shouldThrowExceptionForCompletelyInvalidToken() {
            assertThatThrownBy(() -> jwtService.validateAndExtractClaims("invalid-token"))
                    .isInstanceOf(MalformedJwtException.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrueForValidToken() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When & Then
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            assertThat(jwtService.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            assertThat(jwtService.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("should return false for whitespace-only token")
        void shouldReturnFalseForWhitespaceOnlyToken() {
            assertThat(jwtService.isTokenValid("   ")).isFalse();
        }

        @Test
        @DisplayName("should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertThat(jwtService.isTokenValid("not.a.valid.jwt")).isFalse();
        }

        @Test
        @DisplayName("should return false for token with invalid signature")
        void shouldReturnFalseForTokenWithInvalidSignature() {
            // Given - create a token with a different secret
            JwtConfig differentConfig = new JwtConfig();
            differentConfig.setSecret("different-secret-key-that-is-at-least-32-characters-long");
            differentConfig.setExpiration(TEST_EXPIRATION);
            JwtService differentService = new JwtServiceImpl(differentConfig);

            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String tokenFromDifferentService = differentService.generateToken(user);

            // When & Then - validate with original service should fail
            assertThat(jwtService.isTokenValid(tokenFromDifferentService)).isFalse();
        }

        @Test
        @DisplayName("should return false for tampered token")
        void shouldReturnFalseForTamperedToken() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // Tamper with the token by modifying a character in the payload
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                // Modify the payload part
                String tamperedPayload = parts[1] + "x";
                String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

                // When & Then
                assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Token Expiration")
    class TokenExpiration {

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Given - create a service with very short expiration
            JwtConfig shortExpirationConfig = new JwtConfig();
            shortExpirationConfig.setSecret(TEST_SECRET);
            shortExpirationConfig.setExpiration(1L); // 1 millisecond
            JwtService shortExpirationService = new JwtServiceImpl(shortExpirationConfig);

            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = shortExpirationService.generateToken(user);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When & Then
            assertThat(shortExpirationService.isTokenValid(token)).isFalse();
            assertThatThrownBy(() -> shortExpirationService.validateAndExtractClaims(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("should accept non-expired token")
        void shouldAcceptNonExpiredToken() {
            // Given
            User user = createTestUser(1L, "Test User", "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            // When & Then
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("Round-Trip Validation")
    class RoundTripValidation {

        @Test
        @DisplayName("should preserve all user claims through round-trip")
        void shouldPreserveAllUserClaimsThroughRoundTrip() {
            // Given
            Long userId = 123L;
            String email = "roundtrip@example.com";
            Role role = Role.PROBLEM_SETTER;
            User user = createTestUser(userId, "Round Trip User", email, role);

            // When
            String token = jwtService.generateToken(user);
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Then
            assertThat(claims.get("userId", Long.class)).isEqualTo(userId);
            assertThat(claims.get("email", String.class)).isEqualTo(email);
            assertThat(claims.get("role", String.class)).isEqualTo(role.name());
            assertThat(claims.getSubject()).isEqualTo(email);
        }

        @Test
        @DisplayName("should work for all role types")
        void shouldWorkForAllRoleTypes() {
            for (Role role : Role.values()) {
                // Given
                User user = createTestUser(1L, "Test User", "test@example.com", role);

                // When
                String token = jwtService.generateToken(user);
                Claims claims = jwtService.validateAndExtractClaims(token);

                // Then
                assertThat(claims.get("role", String.class)).isEqualTo(role.name());
            }
        }
    }
}
