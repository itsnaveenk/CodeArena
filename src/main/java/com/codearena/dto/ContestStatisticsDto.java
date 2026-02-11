package com.codearena.dto;

import java.util.Map;

public record ContestStatisticsDto(
    int totalParticipants,
    long totalSubmissions,
    double averageScore,
    Map<Long, Double> problemSolveRates
) {
    public static ContestStatisticsDto of(int totalParticipants, long totalSubmissions,
                                           double averageScore, Map<Long, Double> problemSolveRates) {
        return new ContestStatisticsDto(totalParticipants, totalSubmissions, averageScore, problemSolveRates);
    }

    public static ContestStatisticsDto empty() {
        return new ContestStatisticsDto(0, 0, 0.0, Map.of());
    }

    public double getSolveRateForProblem(Long problemId) {
        return problemSolveRates.getOrDefault(problemId, 0.0);
    }

    public boolean hasParticipants() {
        return totalParticipants > 0;
    }

    public boolean hasSubmissions() {
        return totalSubmissions > 0;
    }

    public double getAverageSubmissionsPerParticipant() {
        if (totalParticipants == 0) {
            return 0.0;
        }
        return (double) totalSubmissions / totalParticipants;
    }
}
