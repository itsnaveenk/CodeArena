package com.codearena.dto;

import java.util.List;

public record ContestRunResponse(
    List<TestcaseResult> results,
    int passedCount,
    int totalCount,
    Double overallRuntime,
    Integer overallMemory
) {
    public record TestcaseResult(
        Long testcaseId,
        String input,
        String expectedOutput,
        String actualOutput,
        boolean passed,
        Double runtime,
        Integer memory,
        String status
    ) {}
}
