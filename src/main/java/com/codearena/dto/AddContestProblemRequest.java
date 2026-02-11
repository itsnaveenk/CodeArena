package com.codearena.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddContestProblemRequest(
    @NotNull(message = "Problem ID is required")
    Long problemId,

    @NotNull(message = "Point value is required")
    @Min(value = 1, message = "Point value must be at least 1")
    @Max(value = 1000, message = "Point value cannot exceed 1000")
    Integer pointValue,

    @Min(value = 1, message = "Display order must be at least 1")
    Integer displayOrder
) {
    public static AddContestProblemRequest of(Long problemId, Integer pointValue) {
        return new AddContestProblemRequest(problemId, pointValue, null);
    }

    public static AddContestProblemRequest of(Long problemId, Integer pointValue, Integer displayOrder) {
        return new AddContestProblemRequest(problemId, pointValue, displayOrder);
    }

    public boolean hasDisplayOrder() {
        return displayOrder != null;
    }
}
