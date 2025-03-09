package com.example.alarms.exceptions;

public class RuleProcessingException extends RuntimeException {
    public RuleProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}