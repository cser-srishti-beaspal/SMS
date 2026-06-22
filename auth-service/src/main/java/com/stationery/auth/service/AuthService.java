package com.stationery.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stationery.auth.dto.AuthResponse;
import com.stationery.auth.dto.LoginRequest;
import com.stationery.auth.dto.RegisterRequest;
import com.stationery.auth.model.Role;
import com.stationery.auth.model.User;
import com.stationery.auth.repository.UserRepository;
import com.stationery.auth.security.JwtUtil;

/**
 * Service handling user registration, login, and token validation logic.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil; // Handles token generation and validation.
    private final AuthenticationManager authenticationManager;//Spring Security's built-in component that verifies user credentials during login.
    private final AuditLogger auditLogger; // A helper class that logs security events (like login/registration) to a log file or audit database.

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.auditLogger = auditLogger;
    }

    /**
     * Registers a new user in the system.
     * Validates that the username and email are not already taken,
     * encodes the password, saves the user, and returns a JWT token.
     *
     * @param request the registration request DTO
     * @return an AuthResponse containing the JWT token and user details
     * @throws RuntimeException if the username or email already exists
     */
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());//convert string -> enum
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role provided: {}, defaulting to STUDENT", request.getRole());
            role = Role.STUDENT;
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))// ENCRYPT PASSWORD
                .role(role)
                .build();

        userRepository.save(user);
        logger.info("User registered successfully: {}", user.getUsername());

        auditLogger.log("USER_REGISTRATION", user.getUsername(), user.getRole().name(), "Successfully registered account with email: " + user.getEmail());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("User registered successfully")
                .build();
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login request DTO
     * @return an AuthResponse containing the JWT token and user details
     */
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting login for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        logger.info("User authenticated successfully: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByUsername(request.getEmail()))
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        auditLogger.log("USER_LOGIN", user.getUsername(), user.getRole().name(), "Successfully authenticated using email: " + user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }

    /**
     * Validates a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}
