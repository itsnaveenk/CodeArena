package com.codearena.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContestValidationException extends RuntimeException {

    private final List<String> errors;

    public ContestValidationException(List<String> errors) {
        super("Contest validation failed: " + String.join(", ", errors));
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public ContestValidationException(String error) {
        super("Contest validation failed: " + error);
        this.errors = new ArrayList<>();
        this.errors.add(error);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public static ContestValidationException noProblems() {
        return new ContestValidationException("At least one problem is required");
    }

    public static ContestValidationException invalidTimes() {
        return new ContestValidationException("End time must be after start time");
    }

    public static ContestValidationException startTimeInPast() {
        return new ContestValidationException("Start time must be in the future");
    }

    public static ContestValidationException invalidRegistrationWindow() {
        return new ContestValidationException("Registration window must end before or at contest start time");
    }

    public static ContestValidationException invalidDuration() {
        return new ContestValidationException("Duration must not exceed contest window");
    }

    public static ContestValidationException invalidProblemStatus(String problemTitle) {
        return new ContestValidationException("Problem '" + problemTitle + "' must be PUBLISHED or PENDING_REVIEW");
    }

    public static ContestValidationException cannotModify(String status) {
        return new ContestValidationException("Cannot modify contest in " + status + " status");
    }

    public static ContestValidationException cannotDelete(String status) {
        return new ContestValidationException("Can only delete contests in DRAFT status, current status: " + status);
    }

    public static ContestValidationException cannotModifyProblems(String status) {
        return new ContestValidationException("Cannot modify problems when contest is " + status);
    }

    public static ContestValidationException duplicateProblem() {
        return new ContestValidationException("Problem already added to this contest");
    }

    public static ContestValidationException invalidProblemForContest() {
        return new ContestValidationException("Problem must be PUBLISHED or PENDING_REVIEW to be added to a contest");
    }
}
