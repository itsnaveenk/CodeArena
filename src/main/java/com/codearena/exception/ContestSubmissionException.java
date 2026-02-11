package com.codearena.exception;

public class ContestSubmissionException extends RuntimeException {

    private final String errorCode;

    public ContestSubmissionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static ContestSubmissionException contestNotRunning() {
        return new ContestSubmissionException("CONTEST_NOT_RUNNING", "Contest is not currently running");
    }

    public static ContestSubmissionException timeExpired() {
        return new ContestSubmissionException("TIME_EXPIRED", "Your contest time has expired");
    }

    public static ContestSubmissionException notStarted() {
        return new ContestSubmissionException("NOT_STARTED", "You must start the contest first");
    }

    public static ContestSubmissionException rateLimited() {
        return new ContestSubmissionException("RATE_LIMITED", "Rate limit exceeded. Please wait before submitting again.");
    }

    public static ContestSubmissionException notRegistered() {
        return new ContestSubmissionException("NOT_REGISTERED", "You are not registered for this contest");
    }

    public static ContestSubmissionException problemNotInContest() {
        return new ContestSubmissionException("PROBLEM_NOT_IN_CONTEST", "Problem is not part of this contest");
    }
}
