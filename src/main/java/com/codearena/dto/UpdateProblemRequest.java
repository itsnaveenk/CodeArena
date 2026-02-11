package com.codearena.dto;

import java.util.List;
import java.util.Map;

import com.codearena.entity.Difficulty;

import jakarta.validation.constraints.Size;

public record UpdateProblemRequest(
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    String title,
    String statement,
    String constraints,
    Difficulty difficulty,
    List<String> tags,
    Map<String, String> starterCode
) {
    public static UpdateProblemRequest empty() {
        return new UpdateProblemRequest(null, null, null, null, null, null);
    }

    public boolean hasTitle() {
        return title != null;
    }

    public boolean hasStatement() {
        return statement != null;
    }

    public boolean hasConstraints() {
        return constraints != null;
    }

    public boolean hasDifficulty() {
        return difficulty != null;
    }

    public boolean hasTags() {
        return tags != null;
    }

    public boolean hasStarterCode() {
        return starterCode != null;
    }

    public boolean hasAnyUpdate() {
        return hasTitle() || hasStatement() || hasConstraints() || hasDifficulty() || hasTags() || hasStarterCode();
    }
}
