package com.codearena.dto;

import java.util.List;

import com.codearena.entity.Difficulty;

public record ProblemFilter(Difficulty difficulty, List<String> tags, String search) {
    public static ProblemFilter empty() {
        return new ProblemFilter(null, null, null);
    }

    public boolean hasAnyCriteria() {
        return difficulty != null || (tags != null && !tags.isEmpty()) || (search != null && !search.isBlank());
    }

    public boolean hasDifficulty() {
        return difficulty != null;
    }

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    public boolean hasSearch() {
        return search != null && !search.isBlank();
    }
}
