package com.codearena.execution;

import java.util.Locale;

public enum ExecutionBackend {
    JUDGE0,
    LOCAL;

    public static ExecutionBackend fromConfig(String value) {
        if (value == null) {
            return JUDGE0;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "judge0" -> JUDGE0;
            case "local" -> LOCAL;
            default -> throw new IllegalArgumentException("Unsupported execution backend: " + value);
        };
    }
}
