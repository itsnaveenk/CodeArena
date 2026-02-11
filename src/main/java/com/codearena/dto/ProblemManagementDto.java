package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;

public record ProblemManagementDto(
    Long id,
    String title,
    String slug,
    Difficulty difficulty,
    ProblemStatus status,
    Long authorId,
    String authorName,
    int testcaseCount,
    long submissionCount,
    LocalDateTime createdAt
) {
    public static ProblemManagementDto fromEntity(Problem problem, long submissionCount) {
        return new ProblemManagementDto(
            problem.getId(),
            problem.getTitle(),
            problem.getSlug(),
            problem.getDifficulty(),
            problem.getStatus(),
            problem.getCreatedBy().getId(),
            problem.getCreatedBy().getName(),
            problem.getTestcases() != null ? problem.getTestcases().size() : 0,
            submissionCount,
            problem.getCreatedAt()
        );
    }
}
