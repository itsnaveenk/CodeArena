package com.codearena.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkActionRequest(
    @NotEmpty(message = "At least one problem ID is required")
    @Size(max = 100, message = "Cannot process more than 100 problems at once")
    List<Long> ids
) {}
