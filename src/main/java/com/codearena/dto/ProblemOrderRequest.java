package com.codearena.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProblemOrderRequest(
    @NotNull(message = "Problem ID is required")
    Long problemId,

    @NotNull(message = "Display order is required")
    @Min(value = 1, message = "Display order must be at least 1")
    Integer displayOrder
) {
    public static ProblemOrderRequest of(Long problemId, Integer displayOrder) {
        return new ProblemOrderRequest(problemId, displayOrder);
    }
}
