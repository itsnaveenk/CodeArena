package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.document.ContestSubmission;
import com.codearena.entity.Verdict;

public record ContestSubmissionDto(
    String id,
    Long contestId,
    Long problemId,
    Integer languageId,
    Verdict verdict,
    Double runtime,
    Integer memory,
    int passedTestcases,
    int totalTestcases,
    double pointsEarned,
    LocalDateTime submittedAt,
    boolean isAutoSubmitted,
    boolean isBest,
    Double previousBest,
    boolean scoreImproved
) {
    public static ContestSubmissionDto fromDocument(ContestSubmission submission) {
        return new ContestSubmissionDto(
            submission.getId(),
            submission.getContestId(),
            submission.getProblemId(),
            submission.getLanguageId(),
            submission.getVerdict(),
            submission.getRuntime(),
            submission.getMemory(),
            submission.getPassedTestcases() != null ? submission.getPassedTestcases() : 0,
            submission.getTotalTestcases() != null ? submission.getTotalTestcases() : 0,
            submission.getPointsEarned() != null ? submission.getPointsEarned() : 0.0,
            submission.getSubmittedAt(),
            submission.getIsAutoSubmitted() != null && submission.getIsAutoSubmitted(),
            false,
            null,
            false
        );
    }

    public static ContestSubmissionDto of(String id, Long contestId, Long problemId,
                                           Integer languageId, Verdict verdict,
                                           Double runtime, Integer memory,
                                           int passedTestcases, int totalTestcases,
                                           double pointsEarned, LocalDateTime submittedAt) {
        return new ContestSubmissionDto(id, contestId, problemId, languageId, verdict,
                                         runtime, memory, passedTestcases, totalTestcases,
                                         pointsEarned, submittedAt, false, false, null, false);
    }

    public boolean isAccepted() {
        return verdict == Verdict.ACCEPTED;
    }

    public boolean hasPartialPoints() {
        return pointsEarned > 0 && !isAccepted();
    }

    public double getPassRate() {
        if (totalTestcases == 0) {
            return 0.0;
        }
        return (double) passedTestcases / totalTestcases * 100.0;
    }

    public String getTestcaseResult() {
        return passedTestcases + "/" + totalTestcases;
    }
}
