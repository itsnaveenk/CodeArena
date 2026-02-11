package com.codearena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judge0Submission(
    String token,
    String stdout,
    String stderr,
    @JsonProperty("compile_output") String compileOutput,
    String message,
    Judge0Status status,
    Double time,
    Integer memory
) {
    public boolean isCompleted() {
        return status != null && !status.isQueued();
    }

    public boolean isAccepted() {
        return status != null && status.id() == Judge0Status.ACCEPTED;
    }

    public boolean isCompilationError() {
        return status != null && status.id() == Judge0Status.COMPILATION_ERROR;
    }

    public boolean isTimeLimitExceeded() {
        return status != null && status.id() == Judge0Status.TIME_LIMIT_EXCEEDED;
    }

    public boolean isRuntimeError() {
        return status != null && status.isRuntimeError();
    }
}
