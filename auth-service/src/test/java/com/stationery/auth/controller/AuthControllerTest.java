package com.stationery.auth.controller;

import com.stationery.auth.dto.AuthResponse;
import com.stationery.auth.dto.LoginRequest;
import com.stationery.auth.dto.RegisterRequest;
import com.stationery.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole("STUDENT");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        authResponse = AuthResponse.builder()
                .token("mockedToken")
                .username("testuser")
                .role("STUDENT")
                .message("Operation successful")
                .build();
    }

    @Test
    void register_Success() {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
        verify(authService, times(1)).register(registerRequest);
    }

    @Test
    void login_Success() {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
        verify(authService, times(1)).login(loginRequest);
    }

    @Test
    void validateToken_Valid() {
        // Arrange
        when(authService.validateToken("validToken")).thenReturn(true);

        // Act
        ResponseEntity<String> response = authController.validateToken("Bearer validToken");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Token is valid", response.getBody());
        verify(authService, times(1)).validateToken("validToken");
    }

    @Test
    void validateToken_InvalidFormat() {
        // Act
        ResponseEntity<String> response = authController.validateToken("InvalidHeaderFormat");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid token format", response.getBody());
        verify(authService, never()).validateToken(anyString());
    }

    @Test
    void validateToken_NullHeader() {
        // Act
        ResponseEntity<String> response = authController.validateToken(null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid token format", response.getBody());
        verify(authService, never()).validateToken(anyString());
    }

    @Test
    void validateToken_InvalidOrExpired() {
        // Arrange
        when(authService.validateToken("expiredToken")).thenReturn(false);

        // Act
        ResponseEntity<String> response = authController.validateToken("Bearer expiredToken");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token is invalid or expired", response.getBody());
        verify(authService, times(1)).validateToken("expiredToken");
    }
}
