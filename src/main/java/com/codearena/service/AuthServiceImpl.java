package com.codearena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.AuthResponse;
import com.codearena.dto.LoginRequest;
import com.codearena.dto.SignupRequest;
import com.codearena.dto.UserDto;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.exception.EmailAlreadyExistsException;
import com.codearena.exception.InvalidCredentialsException;
import com.codearena.repository.UserRepository;
import com.codearena.security.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        logger.info("Processing signup request for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            logger.warn("Signup failed: email already exists: {}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
            request.name(),
            request.email(),
            passwordHash,
            Role.USER
        );

        user = userRepository.save(user);
        logger.info("User created successfully with ID: {}", user.getId());

        String token = jwtService.generateToken(user);

        return AuthResponse.bearer(token, UserDto.fromEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        logger.info("Processing login request for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                logger.warn("Login failed: user not found for email: {}", request.email());
                return new InvalidCredentialsException();
            });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            logger.warn("Login failed: invalid password for email: {}", request.email());
            throw new InvalidCredentialsException();
        }

        logger.info("User authenticated successfully: {}", user.getEmail());

        String token = jwtService.generateToken(user);

        return AuthResponse.bearer(token, UserDto.fromEntity(user));
    }

    PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
