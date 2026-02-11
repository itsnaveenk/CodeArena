package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunRequest(
    @NotNull Long problemId,
    @NotNull Integer languageId,
    @NotBlank String code,
    String customInput
) {}
