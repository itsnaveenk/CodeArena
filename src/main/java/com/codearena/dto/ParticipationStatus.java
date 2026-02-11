package com.codearena.dto;

public record ParticipationStatus(
    boolean canParticipate,
    String reason,
    Long remainingTimeSeconds
) {
    public static final String REASON_NOT_REGISTERED = "Not registered for this contest";

    public static final String REASON_WITHDRAWN = "Registration was withdrawn";

    public static final String REASON_CONTEST_NOT_RUNNING = "Contest is not currently running";

    public static final String REASON_CONTEST_NOT_STARTED = "Contest has not started yet";

    public static final String REASON_CONTEST_ENDED = "Contest has ended";

    public static final String REASON_NOT_STARTED = "You have not started the contest yet";

    public static final String REASON_TIME_EXPIRED = "Your time has expired";

    public static final String REASON_NOT_ENOUGH_TIME = "Contest window closing soon, not enough time remaining";

    public static ParticipationStatus canParticipate(Long remainingTimeSeconds) {
        return new ParticipationStatus(true, null, remainingTimeSeconds);
    }

    public static ParticipationStatus cannotParticipate(String reason) {
        return new ParticipationStatus(false, reason, null);
    }

    public static ParticipationStatus notRegistered() {
        return cannotParticipate(REASON_NOT_REGISTERED);
    }

    public static ParticipationStatus withdrawn() {
        return cannotParticipate(REASON_WITHDRAWN);
    }

    public static ParticipationStatus contestNotRunning() {
        return cannotParticipate(REASON_CONTEST_NOT_RUNNING);
    }

    public static ParticipationStatus contestNotStarted() {
        return cannotParticipate(REASON_CONTEST_NOT_STARTED);
    }

    public static ParticipationStatus contestEnded() {
        return cannotParticipate(REASON_CONTEST_ENDED);
    }

    public static ParticipationStatus notStarted() {
        return cannotParticipate(REASON_NOT_STARTED);
    }

    public static ParticipationStatus timeExpired() {
        return cannotParticipate(REASON_TIME_EXPIRED);
    }

    public static ParticipationStatus notEnoughTime() {
        return cannotParticipate(REASON_NOT_ENOUGH_TIME);
    }

    public boolean hasRemainingTime() {
        return remainingTimeSeconds != null && remainingTimeSeconds > 0;
    }
}
