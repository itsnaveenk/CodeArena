package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.ContestRegistration;
import com.codearena.entity.RegistrationStatus;

public record ContestRegistrationDto(
    Long contestId,
    Long userId,
    RegistrationStatus status,
    LocalDateTime registeredAt,
    LocalDateTime participantStartTime,
    Long remainingTimeSeconds
) {
    public static ContestRegistrationDto fromEntity(ContestRegistration registration) {
        if (registration == null) {
            return null;
        }
        return new ContestRegistrationDto(
            registration.getContest().getId(),
            registration.getUser().getId(),
            registration.getStatus(),
            registration.getRegisteredAt(),
            registration.getParticipantStartTime(),
            registration.getRemainingTimeSeconds()
        );
    }

    public static ContestRegistrationDto of(Long contestId, Long userId, RegistrationStatus status,
                                             LocalDateTime registeredAt, LocalDateTime participantStartTime,
                                             Long remainingTimeSeconds) {
        return new ContestRegistrationDto(contestId, userId, status, registeredAt,
                                           participantStartTime, remainingTimeSeconds);
    }

    public boolean isRegistered() {
        return status == RegistrationStatus.REGISTERED;
    }

    public boolean isWithdrawn() {
        return status == RegistrationStatus.WITHDRAWN;
    }

    public boolean hasStarted() {
        return participantStartTime != null;
    }

    public boolean isTimeExpired() {
        return remainingTimeSeconds != null && remainingTimeSeconds <= 0;
    }

    public boolean canSubmit() {
        return isRegistered() && remainingTimeSeconds != null && remainingTimeSeconds > 0;
    }
}
