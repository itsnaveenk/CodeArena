package com.codearena.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.codearena.entity.ContestLeaderboardEntry;
import com.codearena.entity.MedalType;

public record ContestLeaderboardEntryDto(
    int rank,
    Long userId,
    String username,
    double totalPoints,
    int problemsSolved,
    String totalTime,
    List<ProblemScoreDto> problemScores,
    MedalType medal,
    boolean isCurrentUser
) {
    public static ContestLeaderboardEntryDto fromEntity(ContestLeaderboardEntry entry) {
        return fromEntity(entry, false);
    }

    public static ContestLeaderboardEntryDto fromEntity(ContestLeaderboardEntry entry, boolean isCurrentUser) {
        List<ProblemScoreDto> problemScoreDtos = entry.getProblemScores().stream()
            .map(ProblemScoreDto::fromEntity)
            .collect(Collectors.toList());

        return new ContestLeaderboardEntryDto(
            entry.getRank(),
            entry.getUser().getId(),
            entry.getUser().getName(),
            entry.getTotalPoints(),
            entry.getProblemsSolved(),
            formatTime(entry.getTotalTimeSeconds()),
            problemScoreDtos,
            entry.getMedal(),
            isCurrentUser
        );
    }

    public static ContestLeaderboardEntryDto of(int rank, Long userId, String username,
                                                 double totalPoints, int problemsSolved,
                                                 long totalTimeSeconds, List<ProblemScoreDto> problemScores,
                                                 MedalType medal, boolean isCurrentUser) {
        return new ContestLeaderboardEntryDto(
            rank, userId, username, totalPoints, problemsSolved,
            formatTime(totalTimeSeconds), problemScores, medal, isCurrentUser
        );
    }

    private static String formatTime(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00:00";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public boolean hasMedal() {
        return medal != null;
    }

    public String getMedalEmoji() {
        if (medal == null) {
            return "";
        }
        return switch (medal) {
            case GOLD -> "🥇";
            case SILVER -> "🥈";
            case BRONZE -> "🥉";
        };
    }
}
