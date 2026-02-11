package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestCategory;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.RegistrationStatus;
import com.codearena.entity.ScoringModel;
import com.codearena.entity.TimerMode;

public record ContestDetailDto(
    Long id,
    String title,
    String slug,
    String description,
    ContestStatus status,
    ContestVisibility visibility,
    ContestCategory category,
    TimerMode timerMode,
    ScoringModel scoringModel,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer durationMinutes,
    LocalDateTime registrationStartTime,
    LocalDateTime registrationEndTime,
    Integer maxParticipants,
    Integer leaderboardFreezeMinutes,
    int registeredCount,
    int problemCount,
    int totalPossiblePoints,
    UserSummaryDto createdBy,
    LocalDateTime createdAt,
    boolean isRegistered,
    RegistrationStatus registrationStatus,
    LocalDateTime participantStartTime,
    Long remainingTimeSeconds,
    boolean canRegister,
    boolean canStart,
    boolean canSubmit
) {
    public static ContestDetailDto fromEntity(Contest contest) {
        return fromEntity(contest, null);
    }

    public static ContestDetailDto fromEntity(Contest contest, ContestRegistration registration) {
        boolean isRegistered = registration != null && registration.getStatus() == RegistrationStatus.REGISTERED;
        RegistrationStatus registrationStatus = registration != null ? registration.getStatus() : null;
        LocalDateTime participantStartTime = registration != null ? registration.getParticipantStartTime() : null;
        Long remainingTimeSeconds = registration != null ? registration.getRemainingTimeSeconds() : null;

        boolean canRegister = calculateCanRegister(contest, registration);
        boolean canStart = calculateCanStart(contest, registration);
        boolean canSubmit = calculateCanSubmit(contest, registration);

        return new ContestDetailDto(
            contest.getId(),
            contest.getTitle(),
            contest.getSlug(),
            contest.getDescription(),
            contest.getStatus(),
            contest.getVisibility(),
            contest.getCategory(),
            contest.getTimerMode(),
            contest.getScoringModel(),
            contest.getStartTime(),
            contest.getEndTime(),
            contest.getDurationMinutes(),
            contest.getRegistrationStartTime(),
            contest.getRegistrationEndTime(),
            contest.getMaxParticipants(),
            contest.getLeaderboardFreezeMinutes(),
            contest.getRegisteredCount(),
            contest.getContestProblems().size(),
            contest.getTotalPossiblePoints(),
            UserSummaryDto.fromEntity(contest.getCreatedBy()),
            contest.getCreatedAt(),
            isRegistered,
            registrationStatus,
            participantStartTime,
            remainingTimeSeconds,
            canRegister,
            canStart,
            canSubmit
        );
    }

    public static ContestDetailDto fromEntityWithCounts(Contest contest, ContestRegistration registration,
                                                         int registeredCount, int problemCount, int totalPossiblePoints) {
        boolean isRegistered = registration != null && registration.getStatus() == RegistrationStatus.REGISTERED;
        RegistrationStatus registrationStatus = registration != null ? registration.getStatus() : null;
        LocalDateTime participantStartTime = registration != null ? registration.getParticipantStartTime() : null;
        Long remainingTimeSeconds = registration != null ? registration.getRemainingTimeSeconds() : null;

        boolean canRegister = calculateCanRegister(contest, registration);
        boolean canStart = calculateCanStart(contest, registration);
        boolean canSubmit = calculateCanSubmit(contest, registration);

        return new ContestDetailDto(
            contest.getId(),
            contest.getTitle(),
            contest.getSlug(),
            contest.getDescription(),
            contest.getStatus(),
            contest.getVisibility(),
            contest.getCategory(),
            contest.getTimerMode(),
            contest.getScoringModel(),
            contest.getStartTime(),
            contest.getEndTime(),
            contest.getDurationMinutes(),
            contest.getRegistrationStartTime(),
            contest.getRegistrationEndTime(),
            contest.getMaxParticipants(),
            contest.getLeaderboardFreezeMinutes(),
            registeredCount,
            problemCount,
            totalPossiblePoints,
            UserSummaryDto.fromEntity(contest.getCreatedBy()),
            contest.getCreatedAt(),
            isRegistered,
            registrationStatus,
            participantStartTime,
            remainingTimeSeconds,
            canRegister,
            canStart,
            canSubmit
        );
    }

    private static boolean calculateCanRegister(Contest contest, ContestRegistration registration) {
        if (registration != null && registration.getStatus() == RegistrationStatus.REGISTERED) {
            return false; // Already registered
        }
        return contest.isRegistrationOpen() && 
               contest.getVisibility() == ContestVisibility.PUBLIC;
    }

    private static boolean calculateCanStart(Contest contest, ContestRegistration registration) {
        if (registration == null || registration.getStatus() != RegistrationStatus.REGISTERED) {
            return false; // Not registered
        }
        if (contest.getTimerMode() != TimerMode.INDIVIDUAL) {
            return false; // Only INDIVIDUAL timer needs explicit start
        }
        if (registration.hasStarted()) {
            return false; // Already started
        }
        if (contest.getStatus() != ContestStatus.RUNNING) {
            return false; // Contest not running
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestStart = contest.getEndTime().minusMinutes(contest.getDurationMinutes());
        return now.isBefore(latestStart) || now.isEqual(latestStart);
    }

    private static boolean calculateCanSubmit(Contest contest, ContestRegistration registration) {
        if (registration == null || registration.getStatus() != RegistrationStatus.REGISTERED) {
            return false; // Not registered
        }
        if (contest.getStatus() != ContestStatus.RUNNING) {
            return false; // Contest not running
        }
        if (contest.getTimerMode() == TimerMode.INDIVIDUAL && !registration.hasStarted()) {
            return false; // INDIVIDUAL timer requires explicit start
        }
        Long remaining = registration.getRemainingTimeSeconds();
        return remaining != null && remaining > 0;
    }
}
