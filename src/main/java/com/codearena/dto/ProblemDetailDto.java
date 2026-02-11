package com.codearena.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;

public record ProblemDetailDto(
    Long id,
    String title,
    String slug,
    String statement,
    String constraints,
    Difficulty difficulty,
    List<String> tags,
    ProblemStatus status,
    List<TestcaseDto> examples,
    Map<String, String> starterCode,
    String rejectionReason,
    LocalDateTime rejectedAt
) {
    public static ProblemDetailDto fromEntity(Problem problem, List<TestcaseDto> examples, Map<String, String> starterCode) {
        return new ProblemDetailDto(
            problem.getId(),
            problem.getTitle(),
            problem.getSlug(),
            problem.getStatement(),
            problem.getConstraints(),
            problem.getDifficulty(),
            problem.getTags(),
            problem.getStatus(),
            examples,
            starterCode,
            problem.getRejectionReason(),
            problem.getRejectedAt()
        );
    }
}
