package com.codearena.dto;

import java.util.ArrayList;
import java.util.List;

public record ValidationResult(
    boolean valid,
    List<String> errors
) {
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error));
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int getErrorCount() {
        return errors.size();
    }

    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    public static class Builder {
        private final List<String> errors = new ArrayList<>();

        public Builder addError(String error) {
            errors.add(error);
            return this;
        }

        public Builder addErrorIf(boolean condition, String error) {
            if (condition) {
                errors.add(error);
            }
            return this;
        }

        public Builder addErrors(List<String> errors) {
            this.errors.addAll(errors);
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
        }
    }
}
