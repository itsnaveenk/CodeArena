package com.codearena.exception;

import com.codearena.entity.ContestStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final ContestStatus fromStatus;
    private final ContestStatus toStatus;

    public InvalidStatusTransitionException(ContestStatus fromStatus, ContestStatus toStatus) {
        super("Cannot transition from " + fromStatus + " to " + toStatus);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
        this.fromStatus = null;
        this.toStatus = null;
    }

    public ContestStatus getFromStatus() {
        return fromStatus;
    }

    public ContestStatus getToStatus() {
        return toStatus;
    }
}
