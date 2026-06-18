package com.stationery.inventory.exception;

/**
 * Exception thrown when a stock deduction request exceeds the available quantity.
 * Results in an HTTP 400 response when handled by the GlobalExceptionHandler.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
