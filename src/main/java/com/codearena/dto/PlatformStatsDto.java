package com.codearena.dto;

import java.util.Map;

import com.codearena.entity.Difficulty;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;

public record PlatformStatsDto(
    long totalUsers,
    Map<Role, Long> usersByRole,
    long totalProblems,
    Map<ProblemStatus, Long> problemsByStatus,
    Map<Difficulty, Long> problemsByDifficulty,
    long totalSubmissions,
    long submissionsToday,
    double acceptanceRate
) {}
