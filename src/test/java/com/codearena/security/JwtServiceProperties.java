package com.codearena.security;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codearena.config.JwtConfig;
import com.codearena.entity.Role;
import com.codearena.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for {@link JwtService}.
 * 
 * <p>These tests verify universal properties that should hold across all valid inputs,
 * using jqwik for property-based testing with randomly generated test data.
 * 
 * <p>Validates: Requirements 2.1, 2.4, 2.5
 * 
 * @see JwtService
 * @see JwtServiceImpl
 */
@Label("JwtService Property Tests")
@Tag("codearena-platform")
class JwtServiceProperties {

    private static final String TEST_SECRET = "this-is-a-test-secret-key-that-is-at-least-32-characters-long";
    private static final String DIFFERENT_SECRET = "a-completely-different-secret-key-for-tampering-tests";
    private static final long TEST_EXPIRATION = 86400000L; // 24 hours
    private static final long EXPIRED_EXPIRATION = -1000L; // Already expired

    /**
     * Creates a JwtService instance for testing.
     * 
     * @return a configured JwtService instance
     */
    private JwtService createJwtService() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtConfig.setExpiration(TEST_EXPIRATION);
        return new JwtServiceImpl(jwtConfig);
    }

    /**
     * Property 4: JWT Token Round-Trip
     * 
     * <p>For any valid user, generating a JWT token and then validating/extracting claims
     * from that token SHALL return the original user's ID, email, and role.
     * 
     * <p><b>Validates: Requirements 2.1, 2.5</b>
     * 
     * @param user a randomly generated valid user
     */
    @Property(tries = 100)
    @Label("Property 4: JWT Token Round-Trip")
    @Tag("jwt-round-trip")
    void jwtTokenRoundTrip(@ForAll("validUsers") User user) {
        // Given: A JWT service
        JwtService jwtService = createJwtService();
        
        // When: Generate a token for the user
        String token = jwtService.generateToken(user);
        
        // And: Validate and extract claims from the token
        Claims claims = jwtService.validateAndExtractClaims(token);
        
        // Then: The extracted claims should match the original user's data
        assertThat(claims.get("userId", Long.class))
                .as("userId claim should match original user ID")
                .isEqualTo(user.getId());
        
        assertThat(claims.get("email", String.class))
                .as("email claim should match original user email")
                .isEqualTo(user.getEmail());
        
        assertThat(claims.get("role", String.class))
                .as("role claim should match original user role name")
                .isEqualTo(user.getRole().name());
    }

    /**
     * Property 6: Invalid Token Rejection
     * 
     * <p>For any malformed, tampered, or expired JWT token, the Authorization_Service
     * SHALL reject requests with an unauthorized error.
     * 
     * <p><b>Validates: Requirements 2.4</b>
     * 
     * @param invalidToken a randomly generated invalid token
     */
    @Property(tries = 100)
    @Label("Property 6: Invalid Token Rejection - Malformed Tokens")
    @Tag("invalid-token-rejection")
    void malformedTokensAreRejected(@ForAll("malformedTokens") String invalidToken) {
        // Given: A JWT service
        JwtService jwtService = createJwtService();
        
        // When/Then: The token should be invalid
        assertThat(jwtService.isTokenValid(invalidToken))
                .as("Malformed token should be rejected")
                .isFalse();
    }

    /**
     * Property 6: Invalid Token Rejection - Tampered Tokens
     * 
     * <p>For any valid token that has been tampered with (modified payload or signature),
     * the Authorization_Service SHALL reject the request.
     * 
     * <p><b>Validates: Requirements 2.4</b>
     * 
     * @param user a randomly generated valid user
     */
    @Property(tries = 100)
    @Label("Property 6: Invalid Token Rejection - Tampered Tokens")
    @Tag("invalid-token-rejection")
    void tamperedTokensAreRejected(@ForAll("validUsers") User user) {
        // Given: A JWT service
        JwtService jwtService = createJwtService();
        
        // When: A token is created with a different secret key (simulating tampering)
        String tamperedToken = createTamperedToken(user);
        
        // Then: The tampered token should be rejected
        assertThat(jwtService.isTokenValid(tamperedToken))
                .as("Tampered token (different signature) should be rejected")
                .isFalse();
    }

    /**
     * Property 6: Invalid Token Rejection - Expired Tokens
     * 
     * <p>For any expired JWT token, the Authorization_Service SHALL reject the request.
     * 
     * <p><b>Validates: Requirements 2.4</b>
     * 
     * @param user a randomly generated valid user
     */
    @Property(tries = 100)
    @Label("Property 6: Invalid Token Rejection - Expired Tokens")
    @Tag("invalid-token-rejection")
    void expiredTokensAreRejected(@ForAll("validUsers") User user) {
        // Given: A JWT service configured with already-expired tokens
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret(TEST_SECRET);
        expiredConfig.setExpiration(EXPIRED_EXPIRATION);
        JwtService expiredJwtService = new JwtServiceImpl(expiredConfig);
        
        // When: Generate an expired token
        String expiredToken = expiredJwtService.generateToken(user);
        
        // Then: The expired token should be rejected by the normal service
        JwtService jwtService = createJwtService();
        assertThat(jwtService.isTokenValid(expiredToken))
                .as("Expired token should be rejected")
                .isFalse();
    }

    /**
     * Property 6: Invalid Token Rejection - Modified Payload
     * 
     * <p>For any valid token where the payload has been modified without re-signing,
     * the Authorization_Service SHALL reject the request.
     * 
     * <p><b>Validates: Requirements 2.4</b>
     * 
     * @param user a randomly generated valid user
     */
    @Property(tries = 100)
    @Label("Property 6: Invalid Token Rejection - Modified Payload")
    @Tag("invalid-token-rejection")
    void modifiedPayloadTokensAreRejected(@ForAll("validUsers") User user) {
        // Given: A JWT service and a valid token
        JwtService jwtService = createJwtService();
        String validToken = jwtService.generateToken(user);
        
        // When: The payload is modified (swap middle part with different base64)
        String modifiedToken = modifyTokenPayload(validToken);
        
        // Then: The modified token should be rejected
        assertThat(jwtService.isTokenValid(modifiedToken))
                .as("Token with modified payload should be rejected")
                .isFalse();
    }

    /**
     * Property 6: Invalid Token Rejection - validateAndExtractClaims throws for invalid tokens
     * 
     * <p>For any invalid token, validateAndExtractClaims SHALL throw an exception.
     * 
     * <p><b>Validates: Requirements 2.4</b>
     * 
     * @param invalidToken a randomly generated invalid token
     */
    @Property(tries = 100)
    @Label("Property 6: Invalid Token Rejection - validateAndExtractClaims throws exception")
    @Tag("invalid-token-rejection")
    void validateAndExtractClaimsThrowsForInvalidTokens(@ForAll("malformedTokens") String invalidToken) {
        // Given: A JWT service
        JwtService jwtService = createJwtService();
        
        // When/Then: validateAndExtractClaims should throw an exception for invalid tokens
        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(invalidToken))
                .as("validateAndExtractClaims should throw exception for invalid token")
                .isInstanceOf(Exception.class);
    }

    /**
     * Creates a token signed with a different secret key (tampered signature).
     * 
     * @param user the user to create a token for
     * @return a token with an invalid signature
     */
    private String createTamperedToken(User user) {
        SecretKey differentKey = Keys.hmacShaKeyFor(DIFFERENT_SECRET.getBytes());
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + TEST_EXPIRATION);
        
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(differentKey)
                .compact();
    }

    /**
     * Modifies the payload portion of a JWT token without re-signing.
     * 
     * @param token the original valid token
     * @return a token with modified payload but original signature
     */
    private String modifyTokenPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return token; // Not a valid JWT structure
        }
        
        // Modify the payload by changing a character
        String payload = parts[1];
        if (payload.length() > 5) {
            // Replace a character in the middle of the payload
            char[] chars = payload.toCharArray();
            chars[payload.length() / 2] = chars[payload.length() / 2] == 'A' ? 'B' : 'A';
            payload = new String(chars);
        }
        
        return parts[0] + "." + payload + "." + parts[2];
    }

    /**
     * Provides arbitrary valid users for property-based testing.
     * 
     * <p>Generates users with:
     * <ul>
     *   <li>ID: Long between 1 and 10000</li>
     *   <li>Name: Alphabetic string of 2-50 characters</li>
     *   <li>Email: Valid email format</li>
     *   <li>Role: Any of USER, PROBLEM_SETTER, ADMIN</li>
     * </ul>
     * 
     * @return an Arbitrary that generates valid User instances
     */
    @Provide
    Arbitrary<User> validUsers() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),
                validEmails(),
                Arbitraries.of(Role.class)
        ).as((id, name, email, role) -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash("hashedPw");
            user.setRole(role);
            return user;
        });
    }

    /**
     * Provides arbitrary valid email addresses for property-based testing.
     * 
     * <p>Generates emails in the format: {localPart}@{domain}.{tld}
     * 
     * @return an Arbitrary that generates valid email strings
     */
    @Provide
    Arbitrary<String> validEmails() {
        Arbitrary<String> localPart = Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(20);
        
        Arbitrary<String> domain = Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(15);
        
        Arbitrary<String> tld = Arbitraries.of("com", "org", "net", "io", "dev");
        
        return Combinators.combine(localPart, domain, tld)
                .as((local, dom, t) -> local.toLowerCase() + "@" + dom.toLowerCase() + "." + t);
    }

    /**
     * Provides arbitrary malformed/invalid JWT tokens for property-based testing.
     * 
     * <p>Generates various types of invalid tokens:
     * <ul>
     *   <li>Empty strings</li>
     *   <li>Random strings (not JWT format)</li>
     *   <li>Tokens with wrong number of parts</li>
     *   <li>Tokens with invalid base64 encoding</li>
     *   <li>Tokens with missing parts</li>
     * </ul>
     * 
     * @return an Arbitrary that generates invalid token strings
     */
    @Provide
    Arbitrary<String> malformedTokens() {
        return Arbitraries.oneOf(
                // Empty and whitespace strings
                Arbitraries.of("", " ", "  ", "\t", "\n"),
                
                // Random strings (not JWT format)
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                
                // Strings with dots but not valid JWT structure
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                        .map(s -> s + "." + s),
                
                // Invalid base64 in JWT-like structure
                Arbitraries.strings().withChars("!@#$%^&*()").ofMinLength(5).ofMaxLength(20)
                        .map(s -> s + "." + s + "." + s),
                
                // JWT-like structure with invalid content
                Combinators.combine(
                        Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30),
                        Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30),
                        Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30)
                ).as((header, payload, sig) -> header + "." + payload + "." + sig),
                
                // Tokens with only one or two parts
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50)
                        .map(s -> Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes())),
                
                // Null-like values
                Arbitraries.of("null", "undefined", "none"),
                
                // Tokens with extra parts
                Combinators.combine(
                        Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15),
                        Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15),
                        Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15),
                        Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15)
                ).as((a, b, c, d) -> a + "." + b + "." + c + "." + d)
        );
    }
}
