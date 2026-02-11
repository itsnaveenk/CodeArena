package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.ContestCategory;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.ScoringModel;
import com.codearena.entity.TimerMode;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContestRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title,

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    String description,

    @NotNull(message = "Timer mode is required")
    TimerMode timerMode,

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration cannot exceed 1440 minutes (24 hours)")
    Integer durationMinutes,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    LocalDateTime startTime,

    @NotNull(message = "End time is required")
    LocalDateTime endTime,

    ContestVisibility visibility,

    ContestCategory category,

    ScoringModel scoringModel,

    LocalDateTime registrationStartTime,

    LocalDateTime registrationEndTime,

    @Min(value = 1, message = "Maximum participants must be at least 1")
    Integer maxParticipants,

    @Min(value = 0, message = "Leaderboard freeze minutes cannot be negative")
    Integer leaderboardFreezeMinutes,

    @Min(value = 0, message = "Leaderboard delay seconds cannot be negative")
    Integer leaderboardDelaySeconds
) {
    public static CreateContestRequest of(String title, String description, TimerMode timerMode,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        return new CreateContestRequest(title, description, timerMode, null, startTime, endTime,
                null, null, null, null, null, null, null, null);
    }

    public static CreateContestRequest ofIndividual(String title, String description, int durationMinutes,
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        return new CreateContestRequest(title, description, TimerMode.INDIVIDUAL, durationMinutes,
                startTime, endTime, null, null, null, null, null, null, null, null);
    }

    public boolean hasVisibility() {
        return visibility != null;
    }

    public boolean hasCategory() {
        return category != null;
    }

    public boolean hasScoringModel() {
        return scoringModel != null;
    }

    public boolean hasRegistrationStartTime() {
        return registrationStartTime != null;
    }

    public boolean hasRegistrationEndTime() {
        return registrationEndTime != null;
    }

    public boolean hasMaxParticipants() {
        return maxParticipants != null;
    }

    public boolean hasLeaderboardFreezeMinutes() {
        return leaderboardFreezeMinutes != null;
    }

    public boolean hasLeaderboardDelaySeconds() {
        return leaderboardDelaySeconds != null;
    }
}
