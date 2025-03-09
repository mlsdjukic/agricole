package com.example.alarms.services.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a validation operation
 */
public class ValidationResult {
    private final List<String> errors;

    private ValidationResult(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    /**
     * Create a successful validation result with no errors
     */
    public static ValidationResult success() {
        return new ValidationResult(Collections.emptyList());
    }

    /**
     * Create a failed validation result with the specified errors
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(errors);
    }

    /**
     * Returns true if validation was successful (no errors)
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Returns the list of validation errors
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns a combined error message with all validation errors
     */
    public String getErrorMessage() {
        return String.join("; ", errors);
    }
}