package com.codearena.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codearena.dto.AuthResponse;
import com.codearena.dto.LoginRequest;
import com.codearena.dto.SignupRequest;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.exception.EmailAlreadyExistsException;
import com.codearena.exception.InvalidCredentialsException;
import com.codearena.repository.UserRepository;
import com.codearena.security.JwtService;

/**
 * Unit tests for {@link AuthServiceImpl}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>User registration (signup) - Requirements 1.1, 1.2, 1.5</li>
 *   <li>User authentication (login) - Requirements 2.1, 2.2</li>
 *   <li>Password hashing with bcrypt - Requirement 17.1</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        authService = new AuthServiceImpl(userRepository, jwtService, passwordEncoder);
    }

    @Nested
    @DisplayName("Signup Tests")
    class SignupTests {

        @Test
        @DisplayName("Should create user with role USER and return JWT token")
        void signup_WithValidRequest_CreatesUserWithRoleUser() {
            // Given
            SignupRequest request = new SignupRequest("John Doe", "john@example.com", "password123");
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

            // When
            AuthResponse response = authService.signup(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isNotNull();
            assertThat(response.user().name()).isEqualTo("John Doe");
            assertThat(response.user().email()).isEqualTo("john@example.com");
            assertThat(response.user().role()).isEqualTo(Role.USER);

            // Verify user was saved with correct data
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("John Doe");
            assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
            assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("Should hash password using bcrypt before storing - Requirement 1.5")
        void signup_WithValidRequest_HashesPasswordWithBcrypt() {
            // Given
            String plainPassword = "password123";
            SignupRequest request = new SignupRequest("John Doe", "john@example.com", plainPassword);
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

            // When
            authService.signup(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            // Verify password is hashed (not plaintext)
            assertThat(savedUser.getPasswordHash()).isNotEqualTo(plainPassword);
            // Verify it's a valid bcrypt hash
            assertThat(savedUser.getPasswordHash()).startsWith("$2");
            // Verify the hash matches the original password
            assertThat(passwordEncoder.matches(plainPassword, savedUser.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("Should reject registration with existing email - Requirement 1.2")
        void signup_WithExistingEmail_ThrowsEmailAlreadyExistsException() {
            // Given
            SignupRequest request = new SignupRequest("John Doe", "existing@example.com", "password123");
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");

            // Verify no user was saved
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should generate JWT token for new user - Requirement 2.1")
        void signup_WithValidRequest_GeneratesJwtToken() {
            // Given
            SignupRequest request = new SignupRequest("John Doe", "john@example.com", "password123");
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtService.generateToken(any(User.class))).thenReturn("generated-jwt-token");

            // When
            AuthResponse response = authService.signup(request);

            // Then
            assertThat(response.token()).isEqualTo("generated-jwt-token");
            verify(jwtService).generateToken(any(User.class));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return JWT token for valid credentials - Requirement 2.1")
        void login_WithValidCredentials_ReturnsJwtToken() {
            // Given
            String email = "john@example.com";
            String password = "password123";
            String hashedPassword = passwordEncoder.encode(password);
            
            User user = new User("John Doe", email, hashedPassword, Role.USER);
            user.setId(1L);
            
            LoginRequest request = new LoginRequest(email, password);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("jwt-token");

            // When
            AuthResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isNotNull();
            assertThat(response.user().email()).isEqualTo(email);
        }

        @Test
        @DisplayName("Should reject login with non-existent email - Requirement 2.2")
        void login_WithNonExistentEmail_ThrowsInvalidCredentialsException() {
            // Given
            LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

            // Verify no token was generated
            verify(jwtService, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should reject login with wrong password - Requirement 2.2")
        void login_WithWrongPassword_ThrowsInvalidCredentialsException() {
            // Given
            String email = "john@example.com";
            String correctPassword = "correctPassword";
            String wrongPassword = "wrongPassword";
            String hashedPassword = passwordEncoder.encode(correctPassword);
            
            User user = new User("John Doe", email, hashedPassword, Role.USER);
            user.setId(1L);
            
            LoginRequest request = new LoginRequest(email, wrongPassword);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

            // Verify no token was generated
            verify(jwtService, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should return user information in response")
        void login_WithValidCredentials_ReturnsUserInfo() {
            // Given
            String email = "john@example.com";
            String password = "password123";
            String hashedPassword = passwordEncoder.encode(password);
            LocalDateTime createdAt = LocalDateTime.now();
            
            User user = new User("John Doe", email, hashedPassword, Role.PROBLEM_SETTER);
            user.setId(42L);
            user.setCreatedAt(createdAt);
            
            LoginRequest request = new LoginRequest(email, password);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("jwt-token");

            // When
            AuthResponse response = authService.login(request);

            // Then
            assertThat(response.user().id()).isEqualTo(42L);
            assertThat(response.user().name()).isEqualTo("John Doe");
            assertThat(response.user().email()).isEqualTo(email);
            assertThat(response.user().role()).isEqualTo(Role.PROBLEM_SETTER);
            assertThat(response.user().createdAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("Bcrypt Cost Factor Tests")
    class BcryptCostFactorTests {

        @Test
        @DisplayName("Should use bcrypt with cost factor of at least 10 - Requirement 17.1")
        void passwordEncoder_UsesBcryptWithCostFactorAtLeast10() {
            // Given
            String password = "testPassword123";
            
            // When
            String hash = authService.getPasswordEncoder().encode(password);
            
            // Then
            // BCrypt hash format: $2a$XX$... where XX is the cost factor
            assertThat(hash).startsWith("$2");
            
            // Extract cost factor from hash
            String[] parts = hash.split("\\$");
            int costFactor = Integer.parseInt(parts[2]);
            
            assertThat(costFactor).isGreaterThanOrEqualTo(10);
        }
    }
}
