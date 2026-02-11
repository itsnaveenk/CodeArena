package com.codearena.exception;

public class ContestRegistrationException extends RuntimeException {

    private final String errorCode;

    public ContestRegistrationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static ContestRegistrationException registrationNotOpen(String reason) {
        return new ContestRegistrationException("REGISTRATION_NOT_OPEN", reason);
    }

    public static ContestRegistrationException alreadyRegistered() {
        return new ContestRegistrationException("ALREADY_REGISTERED", "Already registered for this contest");
    }

    public static ContestRegistrationException notRegistered() {
        return new ContestRegistrationException("NOT_REGISTERED", "You are not registered for this contest");
    }

    public static ContestRegistrationException contestFull() {
        return new ContestRegistrationException("CONTEST_FULL", "Contest has reached maximum capacity");
    }

    public static ContestRegistrationException privateContest() {
        return new ContestRegistrationException("PRIVATE_CONTEST", "Private contest - invitation required");
    }

    public static ContestRegistrationException cannotStart(String reason) {
        return new ContestRegistrationException("CANNOT_START", reason);
    }

    public static ContestRegistrationException alreadyStarted() {
        return new ContestRegistrationException("ALREADY_STARTED", "You have already started this contest");
    }
}
