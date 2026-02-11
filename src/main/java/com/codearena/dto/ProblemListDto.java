package com.codearena.dto;

import java.util.List;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;

public record ProblemListDto(Long id, String title, String slug, Difficulty difficulty, List<String> tags, ProblemSolveStatus solveStatus) {
    public static ProblemListDto fromEntity(Problem problem) {
        return fromEntity(problem, null);
    }

    public static ProblemListDto fromEntity(Problem problem, ProblemSolveStatus solveStatus) {
        return new ProblemListDto(problem.getId(), problem.getTitle(), problem.getSlug(), problem.getDifficulty(), problem.getTags(), solveStatus);
    }
}
