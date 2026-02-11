package com.codearena.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

public record UpdateContestRequest(
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title,

    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    String description,

    LocalDateTime registrationStartTime,

    LocalDateTime registrationEndTime
) {
    public static UpdateContestRequest empty() {
        return new UpdateContestRequest(null, null, null, null);
    }

    public static UpdateContestRequest withDescription(String description) {
        return new UpdateContestRequest(null, description, null, null);
    }

    public boolean hasTitle() {
        return title != null;
    }

    public boolean hasDescription() {
        return description != null;
    }

    public boolean hasRegistrationStartTime() {
        return registrationStartTime != null;
    }

    public boolean hasRegistrationEndTime() {
        return registrationEndTime != null;
    }

    public boolean hasAnyUpdate() {
        return hasTitle() || hasDescription() || hasRegistrationStartTime() || hasRegistrationEndTime();
    }
}
