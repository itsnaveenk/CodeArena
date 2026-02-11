package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestCategory;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.RegistrationStatus;
import com.codearena.entity.TimerMode;

public record ContestListDto(
    Long id,
    String title,
    String slug,
    ContestStatus status,
    ContestVisibility visibility,
    ContestCategory category,
    TimerMode timerMode,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer durationMinutes,
    int registeredCount,
    Integer maxParticipants,
    int problemCount,
    int totalPossiblePoints,
    String createdByName,
    boolean isRegistered,
    RegistrationStatus registrationStatus
) {
    public static ContestListDto fromEntity(Contest contest) {
        return fromEntity(contest, false, null);
    }

    public static ContestListDto fromEntity(Contest contest, boolean isRegistered, RegistrationStatus registrationStatus) {
        return new ContestListDto(
            contest.getId(),
            contest.getTitle(),
            contest.getSlug(),
            contest.getStatus(),
            contest.getVisibility(),
            contest.getCategory(),
            contest.getTimerMode(),
            contest.getStartTime(),
            contest.getEndTime(),
            contest.getDurationMinutes(),
            contest.getRegisteredCount(),
            contest.getMaxParticipants(),
            contest.getContestProblems().size(),
            contest.getTotalPossiblePoints(),
            contest.getCreatedBy() != null ? contest.getCreatedBy().getName() : null,
            isRegistered,
            registrationStatus
        );
    }

    public static ContestListDto fromEntityWithCounts(Contest contest, int registeredCount, int problemCount,
                                                       int totalPossiblePoints, boolean isRegistered,
                                                       RegistrationStatus registrationStatus) {
        return new ContestListDto(
            contest.getId(),
            contest.getTitle(),
            contest.getSlug(),
            contest.getStatus(),
            contest.getVisibility(),
            contest.getCategory(),
            contest.getTimerMode(),
            contest.getStartTime(),
            contest.getEndTime(),
            contest.getDurationMinutes(),
            registeredCount,
            contest.getMaxParticipants(),
            problemCount,
            totalPossiblePoints,
            contest.getCreatedBy() != null ? contest.getCreatedBy().getName() : null,
            isRegistered,
            registrationStatus
        );
    }
}
