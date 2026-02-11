package com.codearena.entity;

import java.util.Objects;

public class ProblemScore {

    private Long problemId;
    private Double bestPoints;
    private Long solveTimeSeconds;
    private Integer submissionCount;

    public ProblemScore() {
    }

    public ProblemScore(Long problemId, Double bestPoints, Long solveTimeSeconds) {
        this(problemId, bestPoints, solveTimeSeconds, 1);
    }

    public ProblemScore(Long problemId, Double bestPoints, Long solveTimeSeconds, Integer submissionCount) {
        this.problemId = problemId;
        this.bestPoints = bestPoints;
        this.solveTimeSeconds = solveTimeSeconds;
        this.submissionCount = submissionCount;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Double getBestPoints() {
        return bestPoints;
    }

    public void setBestPoints(Double bestPoints) {
        this.bestPoints = bestPoints;
    }

    public Long getSolveTimeSeconds() {
        return solveTimeSeconds;
    }

    public void setSolveTimeSeconds(Long solveTimeSeconds) {
        this.solveTimeSeconds = solveTimeSeconds;
    }

    public Integer getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(Integer submissionCount) {
        this.submissionCount = submissionCount;
    }

    public void incrementSubmissionCount() {
        this.submissionCount = (this.submissionCount != null ? this.submissionCount : 0) + 1;
    }

    @Override
    public String toString() {
        return "ProblemScore{problemId=" + problemId + ", bestPoints=" + bestPoints + 
               ", solveTimeSeconds=" + solveTimeSeconds + 
               ", submissionCount=" + submissionCount + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProblemScore that = (ProblemScore) o;
        return Objects.equals(problemId, that.problemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemId);
    }
}
