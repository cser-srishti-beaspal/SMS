package com.stationery.auth.controller;

// DTOs — Data Transfer Objects (sirf data carry karte hain, koi logic nahi)
import org.slf4j.Logger;      // Response: JWT token client ko bhejne ke liye
import org.slf4j.LoggerFactory;       // Request: email + password
import org.springframework.http.HttpStatus;    // Request: username + email + password + role
import org.springframework.http.ResponseEntity;    // Actual business logic yahan hai (register, login, validate)
import org.springframework.web.bind.annotation.GetMapping;                   // @Valid → RequestBody pe validation trigger karta hai
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;        // HTTP codes: 200, 201, 401 etc.
import org.springframework.web.bind.annotation.RequestMapping;    // Status + Body dono saath return karne ke liye
import org.springframework.web.bind.annotation.RestController; // @RestController, @RequestMapping, @PostMapping etc. sab ek saath

import com.stationery.auth.dto.AuthResponse;
import com.stationery.auth.dto.LoginRequest;
import com.stationery.auth.dto.RegisterRequest;
import com.stationery.auth.service.AuthService;

import jakarta.validation.Valid;

/**
 * AuthController — Authentication ka entry point.
 * Teenon endpoints yahan hain:
 *   POST /api/auth/register  → Naya user banao
 *   POST /api/auth/login     → Login karo, JWT lo
 *   GET  /api/auth/validate  → Token valid hai ya nahi check karo
 *
 * Controller sirf traffic manage karta hai —
 *                asli kaam AuthService karta hai (Single Responsibility Principle)
 */
@RestController   // = @Controller + @ResponseBody → methods JSON return karte hain automatically
@RequestMapping("/api/auth") // Saare endpoints ka base URL — "/api/auth/..."
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // AuthService ko inject karo — yahan business logic nahi likhte
    private final AuthService authService;

    // Constructor Injection — @Autowired se better hai (testable + immutable)
    // Field injection (@Autowired) se constructor injection preferred hai
    //                kyunki unit test mein mock inject karna easy hota hai
    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    // ─────────────────────────────────────────────────────────────
    // ENDPOINT 1: REGISTER
    // POST /api/auth/register
    // Naya user system mein register karo
    // Success pe → 201 CREATED + JWT token
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid          // RegisterRequest ke @NotBlank, @Email jaise annotations trigger honge
            @RequestBody    // HTTP request ka JSON body → RegisterRequest object mein convert karo
            RegisterRequest request) {

        // Request aayi — pehle log karo (debug ke liye useful)
        logger.info("Registration request received for username: {}", request.getUsername());

        // Saara kaam AuthService karta hai — controller sirf delegate karta hai
        AuthResponse response = authService.register(request);

        logger.info("Registration successful for username: {}", request.getUsername());

        // 201 CREATED — 200 OK nahi, kyunki naya resource (user) create hua hai
        // INTERVIEW TIP: POST se naya resource bane → 201, warna → 200
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // ─────────────────────────────────────────────────────────────
    // ENDPOINT 2: LOGIN
    // POST /api/auth/login
    // Email + Password lo, verify karo, JWT token do
    // Success pe → 200 OK + JWT token
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid        // LoginRequest ke validations check hogi (@NotBlank etc.)
            @RequestBody  // JSON body → LoginRequest object
            LoginRequest request) {

        logger.info("Login request received for email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        logger.info("Login successful for email: {}", request.getEmail());

        // ResponseEntity.ok() = 200 OK shortcut — login pe naya resource nahi banta
        return ResponseEntity.ok(response);
    }


    // ─────────────────────────────────────────────────────────────
    // ENDPOINT 3: VALIDATE TOKEN
    // GET /api/auth/validate
    // Header mein JWT token bhejo, hum batayenge valid hai ya nahi
    //
    // Expected Header: Authorization: Bearer eyJhbGci...
    //                                 ↑ "Bearer " = 7 characters (substring(7) yahan se aaya)
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(
            @RequestHeader("Authorization") // HTTP Header se "Authorization" ki value nikalo
            String authHeader) {            // → "Bearer eyJhbGciOiJIUzI1NiJ9..."

        logger.info("Token validation request received");

        // STEP 1: Header format check karo — do conditions:
        //   a) null nahi hona chahiye
        //   b) "Bearer " se start hona chahiye (space bhi count hoti hai!)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Token validation failed - invalid Authorization header format");
            // 401 UNAUTHORIZED — token hai hi nahi sahi format mein
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token format");
        }

        // STEP 2: "Bearer " hata ke sirf token nikalo
        // "Bearer eyJhbGci..." → index 7 se start karo → "eyJhbGci..."
        //  0123456↑ (7 characters = "Bearer ")
        String token = authHeader.substring(7);

        // STEP 3: AuthService se validate karo (signature + expiry dono check hote hain)
        boolean isValid = authService.validateToken(token);

        // STEP 4: Result ke hisaab se response do
        if (isValid) {
            logger.info("Token validation successful");
            return ResponseEntity.ok("Token is valid"); // 200 ✅
        } else {
            logger.warn("Token validation failed - token is invalid or expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired"); // 401 ❌
        }
    }
}