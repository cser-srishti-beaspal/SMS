package com.stationery.auth.service;

import com.stationery.auth.dto.AuthResponse;
import com.stationery.auth.dto.LoginRequest;
import com.stationery.auth.dto.RegisterRequest;
import com.stationery.auth.model.Role;
import com.stationery.auth.model.User;
import com.stationery.auth.repository.UserRepository;
import com.stationery.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole("STUDENT");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateToken(eq("testuser"), eq("STUDENT"))).thenReturn("mockedToken");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mockedToken", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("STUDENT", response.getRole());
        assertEquals("User registered successfully", response.getMessage());

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken("testuser", "STUDENT");
    }

    @Test
    void register_DuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Username already exists: testuser", exception.getMessage());

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Email already exists: test@example.com", exception.getMessage());

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuthentication);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(sampleUser));
        when(jwtUtil.generateToken("testuser", "STUDENT")).thenReturn("mockedToken");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mockedToken", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("STUDENT", response.getRole());
        assertEquals("Login successful", response.getMessage());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(jwtUtil, times(1)).generateToken("testuser", "STUDENT");
    }

    @Test
    void login_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }
}
