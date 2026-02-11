package com.codearena.exception;

import java.util.HashMap;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, String> details;

    public ValidationException(String message) {
        super(message);
        this.details = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> details) {
        super(message);
        this.details = details != null ? details : new HashMap<>();
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
