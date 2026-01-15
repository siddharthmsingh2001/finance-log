package com.financelog.core;

/**
 * Utility class for common validation logic, used primarily to validate
 * CDK context variables at the start of the application.
 */
public class Validations {

    /**
     * Ensures a string is neither null nor contains only whitespace.
     * * @param string The value to check.
     * @param message The error message to display if validation fails.
     * @throws IllegalArgumentException if validation fails.
     */
    public static void requireNonEmpty(String string, String message) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}