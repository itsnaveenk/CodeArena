package com.codearena.dto;

import java.util.List;
import java.util.Map;

import com.codearena.entity.ContestProblem;
import com.codearena.entity.Problem;

public record ContestProblemDetailDto(
    Long id,
    Long problemId,
    String title,
    String statement,
    String constraints,
    int pointValue,
    int displayOrder,
    List<TestcaseDto> examples,
    Map<String, String> starterCode,
    int solveCount,
    Double userBestPoints,
    int userSubmissionCount
) {
    public static ContestProblemDetailDto fromEntity(ContestProblem contestProblem,
                                                      List<TestcaseDto> examples,
                                                      Map<String, String> starterCode) {
        return fromEntity(contestProblem, examples, starterCode, 0, null, 0);
    }

    public static ContestProblemDetailDto fromEntity(ContestProblem contestProblem,
                                                      List<TestcaseDto> examples,
                                                      Map<String, String> starterCode,
                                                      int solveCount,
                                                      Double userBestPoints,
                                                      int userSubmissionCount) {
        Problem problem = contestProblem.getProblem();
        return new ContestProblemDetailDto(
            contestProblem.getId(),
            problem.getId(),
            problem.getTitle(),
            problem.getStatement(),
            problem.getConstraints(),
            contestProblem.getPointValue(),
            contestProblem.getDisplayOrder(),
            examples,
            starterCode,
            solveCount,
            userBestPoints,
            userSubmissionCount
        );
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
