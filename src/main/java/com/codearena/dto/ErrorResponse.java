package com.codearena.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(String code, String message, LocalDateTime timestamp, String path, Map<String, String> details) {
    public ErrorResponse(String code, String message, String path) {
        this(code, message, LocalDateTime.now(), path, null);
    }

    public ErrorResponse(String code, String message, String path, Map<String, String> details) {
        this(code, message, LocalDateTime.now(), path, details);
    }
}
