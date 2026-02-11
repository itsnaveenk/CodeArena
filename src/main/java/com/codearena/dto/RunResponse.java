package com.codearena.dto;

public record RunResponse(String stdout, String stderr, String compileOutput, ExecutionStatus status, Double time, Integer memory, String expectedOutput) {
    public static RunResponse success(String stdout, Double time, Integer memory) {
        return new RunResponse(stdout, null, null, ExecutionStatus.SUCCESS, time, memory, null);
    }

    public static RunResponse accepted(String stdout, Double time, Integer memory, String expectedOutput) {
        return new RunResponse(stdout, null, null, ExecutionStatus.ACCEPTED, time, memory, expectedOutput);
    }

    public static RunResponse wrongAnswer(String stdout, Double time, Integer memory, String expectedOutput) {
        return new RunResponse(stdout, null, null, ExecutionStatus.WRONG_ANSWER, time, memory, expectedOutput);
    }

    public static RunResponse compilationError(String compileOutput) {
        return new RunResponse(null, null, compileOutput, ExecutionStatus.COMPILATION_ERROR, null, null, null);
    }

    public static RunResponse runtimeError(String stderr, Double time, Integer memory) {
        return new RunResponse(null, stderr, null, ExecutionStatus.RUNTIME_ERROR, time, memory, null);
    }

    public static RunResponse timeLimitExceeded(Double time, Integer memory) {
        return new RunResponse(null, null, null, ExecutionStatus.TIME_LIMIT_EXCEEDED, time, memory, null);
    }
}
