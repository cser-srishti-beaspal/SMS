package com.stationery.inventory.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 * Results in an HTTP 404 response when handled by the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
