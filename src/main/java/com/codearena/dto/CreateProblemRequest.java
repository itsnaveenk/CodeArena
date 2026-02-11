package com.codearena.dto;

import java.util.List;
import java.util.Map;

import com.codearena.entity.Difficulty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProblemRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    String title,

    @NotBlank(message = "Statement is required")
    String statement,

    String constraints,

    @NotNull(message = "Difficulty is required")
    Difficulty difficulty,

    List<String> tags,

    Map<String, String> starterCode
) {
    public static CreateProblemRequest of(String title, String statement, Difficulty difficulty) {
        return new CreateProblemRequest(title, statement, null, difficulty, null, null);
    }

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    public boolean hasStarterCode() {
        return starterCode != null && !starterCode.isEmpty();
    }
}
