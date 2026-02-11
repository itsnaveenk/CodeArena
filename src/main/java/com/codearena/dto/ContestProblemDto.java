package com.codearena.dto;

import com.codearena.entity.ContestProblem;

public record ContestProblemDto(
    Long id,
    Long problemId,
    String title,
    int pointValue,
    int displayOrder,
    int solveCount,
    Double userBestPoints,
    int userSubmissionCount
) {
    public static ContestProblemDto fromEntity(ContestProblem contestProblem) {
        return fromEntity(contestProblem, 0, null, 0);
    }

    public static ContestProblemDto fromEntity(ContestProblem contestProblem, int solveCount,
                                                Double userBestPoints, int userSubmissionCount) {
        return new ContestProblemDto(
            contestProblem.getId(),
            contestProblem.getProblem().getId(),
            contestProblem.getProblem().getTitle(),
            contestProblem.getPointValue(),
            contestProblem.getDisplayOrder(),
            solveCount,
            userBestPoints,
            userSubmissionCount
        );
    }

    public static ContestProblemDto of(Long id, Long problemId, String title, int pointValue,
                                        int displayOrder, int solveCount, Double userBestPoints,
                                        int userSubmissionCount) {
        return new ContestProblemDto(id, problemId, title, pointValue, displayOrder,
                                      solveCount, userBestPoints, userSubmissionCount);
    }

    public boolean isAttempted() {
        return userSubmissionCount > 0;
    }

    public boolean isSolved() {
        return userBestPoints != null && userBestPoints >= pointValue;
    }

    public boolean hasPartialPoints() {
        return userBestPoints != null && userBestPoints > 0 && userBestPoints < pointValue;
    }
}
