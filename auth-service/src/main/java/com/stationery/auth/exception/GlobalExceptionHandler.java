package com.stationery.auth.exception;

import java.time.LocalDateTime;          // Error response mein exact time dene ke liye
import java.util.stream.Collectors;      // List of errors ko ek string mein join karne ke liye

import org.slf4j.Logger;                 // Logger ka type (interface)
import org.slf4j.LoggerFactory;          // Logger ka object banane ki factory
import org.springframework.http.HttpStatus;               // HTTP codes: 200, 400, 401, 500 etc.
import org.springframework.http.ResponseEntity;           // Poora HTTP response wrap karta hai (status + body)
import org.springframework.security.core.AuthenticationException; // Login fail hone par aata hai
import org.springframework.web.bind.MethodArgumentNotValidException; // @Valid validation fail hone par aata hai
import org.springframework.web.bind.annotation.ExceptionHandler;    // Batata hai - "yeh error aaye toh yeh method chalaao"
import org.springframework.web.bind.annotation.RestControllerAdvice; // Global error catcher - poori app ke errors yahan aate hain

/**
 * Global Exception Handler — Auth Service ka centralized error management.
 * Koi bhi unhandled exception directly yahan aati hai aur
 * clean JSON ErrorResponse mein convert ho ke client ko jaati hai.
 *
 * Bina is class ke, Spring apna default ugly error page bhejta.
 *                Is class se hum khud control karte hain ki client ko kya dikhana hai.
 */
@RestControllerAdvice // = @ControllerAdvice + @ResponseBody; sabhi controllers pe globally apply hota hai
public class GlobalExceptionHandler {

    // Ek baar logger banao, pure class mein reuse karo — logs console/file mein jaate hain
    // LoggerFactory.getLogger(X.class) → logs mein class ka naam dikhata hai, debugging easy hoti hai
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    // ─────────────────────────────────────────────────────────────
    // HANDLER 1: RuntimeException
    // Kab aata hai? — Duplicate username/email, business logic violations
    // Response Code? — 400 BAD REQUEST
    // ─────────────────────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class) // Yeh method sirf RuntimeException ke liye trigger hoga
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {

        // Server-side log: developer ke liye — client ko nahi dikhta
        logger.error("RuntimeException occurred: {}", ex.getMessage()); // {} = placeholder, ex.getMessage() inject hoga

        // Builder pattern se ErrorResponse object banana — clean aur readable
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value()) // 400 (int value)
                .message(ex.getMessage())               // Exception ki actual message
                .timestamp(LocalDateTime.now())         // Exact time jab error hua
                .build();

        // ResponseEntity = HTTP status (400) + body (errorResponse JSON) dono saath bhejo
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    // ─────────────────────────────────────────────────────────────
    // HANDLER 2: MethodArgumentNotValidException
    // Kab aata hai? — @Valid fail ho jaye, e.g. blank email, weak password
    // Response Code? — 400 BAD REQUEST (lekin multiple field errors hote hain)
    // ─────────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        logger.error("Validation error occurred: {}", ex.getMessage());

        // ex.getBindingResult() → sabhi validation failures ka container
        // .getFieldErrors()     → specific field wise errors ki list (e.g. "email", "password")
        // .stream()             → list ko process karne ke liye stream mein convert karo
        // .map(...)             → har error ko "fieldName: errorMessage" string mein convert karo
        // .collect(joining(,))  → saari strings ko ek string mein join karo — "email: blank, password: weak"
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value()) // 400
                .message(errorMessages)                 // "email: must not be blank, password: too short"
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    // ─────────────────────────────────────────────────────────────
    // HANDLER 3: AuthenticationException
    // Kab aata hai? — Wrong password, expired/invalid JWT token
    // Response Code? — 401 UNAUTHORIZED (400 nahi! — yeh auth failure hai)
    // ─────────────────────────────────────────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {

        logger.error("Authentication error occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())                    // 401
                .message("Authentication failed: " + ex.getMessage())       // "Authentication failed: Bad credentials"
                .timestamp(LocalDateTime.now())
                .build();

        // 401 = "Tu kaun hai? Pehle login kar" — 403 hota "tera access nahi hai"
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}