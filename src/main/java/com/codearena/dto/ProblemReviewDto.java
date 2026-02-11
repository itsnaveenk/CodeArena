package com.codearena.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;

public record ProblemReviewDto(
    Long id,
    String title,
    String slug,
    String statement,
    String constraints,
    Difficulty difficulty,
    List<String> tags,
    ProblemStatus status,
    Long authorId,
    String authorName,
    String authorEmail,
    List<TestcaseDto> testcases,
    Map<String, String> starterCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int testcaseCount,
    int visibleTestcaseCount,
    int hiddenTestcaseCount,
    long submissionCount,
    String rejectionReason,
    LocalDateTime rejectedAt,
    String rejectedByName
) {
    public static ProblemReviewDto fromEntity(Problem problem, List<TestcaseDto> testcases, Map<String, String> starterCodeMap, long submissionCount) {
        long visibleCount = testcases.stream().filter(tc -> !tc.isHidden()).count();
        long hiddenCount = testcases.stream().filter(TestcaseDto::isHidden).count();

        return new ProblemReviewDto(
            problem.getId(),
            problem.getTitle(),
            problem.getSlug(),
            problem.getStatement(),
            problem.getConstraints(),
            problem.getDifficulty(),
            problem.getTags(),
            problem.getStatus(),
            problem.getCreatedBy().getId(),
            problem.getCreatedBy().getName(),
            problem.getCreatedBy().getEmail(),
            testcases,
            starterCodeMap,
            problem.getCreatedAt(),
            problem.getUpdatedAt(),
            testcases.size(),
            (int) visibleCount,
            (int) hiddenCount,
            submissionCount,
            problem.getRejectionReason(),
            problem.getRejectedAt(),
            problem.getRejectedBy() != null ? problem.getRejectedBy().getName() : null
        );
    }
}
