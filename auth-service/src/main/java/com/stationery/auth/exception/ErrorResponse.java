package com.stationery.auth.exception;

import java.time.LocalDateTime; // Error hua — exact kab? yeh batane ke liye

/**
 * ErrorResponse — Jab bhi koi exception aata hai, client ko yeh object JSON mein milta hai.
 *
 * Client ko milne wala response kuch aisa dikhta hai:
 * {
 *   "status"    : 400,
 *   "message"   : "Email must not be blank",
 *   "timestamp" : "2025-06-23T14:30:00"
 * }
 *
 *  Yeh ek simple POJO (Plain Old Java Object) hai —
 *                koi Spring magic nahi, sirf data hold karta hai.
 */
public class ErrorResponse {

    // ─── 3 Fields — Error ka poora summary ───────────────────────
    private int status;              // HTTP code    → 400, 401, 500
    private String message;          // Kya hua?     → "Duplicate email"
    private LocalDateTime timestamp; // Kab hua?     → "2025-06-23T14:30:00"


    // ─── Constructors ─────────────────────────────────────────────

    // No-arg constructor — Spring/Jackson ko JSON → Object convert karne ke liye chahiye
    public ErrorResponse() {}

    // Parameterized constructor — Builder ke build() method se call hota hai
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }


    // ─── Getters & Setters ────────────────────────────────────────
    // Jackson (Spring ka JSON converter) inhi getters se JSON banata hai

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }


    // ─── Builder Pattern Entry Point ──────────────────────────────
    // ErrorResponse.builder() → Builder object milta hai, phir chain karo
    // Lombok ka @Builder yahi sab auto-generate karta hai
    public static ErrorResponseBuilder builder() { return new ErrorResponseBuilder(); }


    // ─────────────────────────────────────────────────────────────
    // INNER CLASS: ErrorResponseBuilder
    //
    // Builder Pattern kya hai?
    // Seedha new ErrorResponse(400, "msg", time) likhne ki jagah
    // step-by-step chain karke object banao — readable + flexible
    //
    // Usage (GlobalExceptionHandler mein dekha tha):
    //   ErrorResponse.builder()
    //       .status(400)
    //       .message("Something went wrong")
    //       .timestamp(LocalDateTime.now())
    //       .build(); ← yahan actual object banta hai
    // ─────────────────────────────────────────────────────────────
    public static class ErrorResponseBuilder {

        // Builder ke apne temporary fields — jab tak build() na ho, object nahi banta
        private int status;
        private String message;
        private LocalDateTime timestamp;

        // Har setter 'this' (builder) return karta hai → isliye chaining possible hai
        // .status(400).message("err").timestamp(now())  ← yahi chaining hai
        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this; // Builder wapas karo taaki agli method chain ho sake
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        // Saare collected values se actual ErrorResponse object banao aur return karo
        public ErrorResponse build() {
            return new ErrorResponse(status, message, timestamp);
        }
    }
}