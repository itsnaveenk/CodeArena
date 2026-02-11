package com.codearena.dto;

public record LeaderboardEntryDto(int rank, Long userId, String name, String email, long solved, Double bestRuntime) {}
