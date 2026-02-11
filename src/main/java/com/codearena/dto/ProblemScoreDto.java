package com.codearena.dto;

import com.codearena.entity.ProblemScore;

public record ProblemScoreDto(
    Long problemId,
    double points,
    String solveTime
) {
    public static ProblemScoreDto fromEntity(ProblemScore problemScore) {
        if (problemScore == null) {
            return null;
        }
        return new ProblemScoreDto(
            problemScore.getProblemId(),
            problemScore.getBestPoints() != null ? problemScore.getBestPoints() : 0.0,
            formatTime(problemScore.getSolveTimeSeconds())
        );
    }

    public static ProblemScoreDto of(Long problemId, double points, Long solveTimeSeconds) {
        return new ProblemScoreDto(problemId, points, formatTime(solveTimeSeconds));
    }

    public static ProblemScoreDto unattempted(Long problemId) {
        return new ProblemScoreDto(problemId, 0.0, null);
    }

    private static String formatTime(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public boolean isAttempted() {
        return points > 0 || solveTime != null;
    }

    public boolean isSolved() {
        return points > 0;
    }
}
