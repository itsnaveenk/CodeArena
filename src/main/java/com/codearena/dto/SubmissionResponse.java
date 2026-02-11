package com.codearena.dto;

import com.codearena.entity.Verdict;

public record SubmissionResponse(String submissionId, Verdict verdict, Double runtime, Integer memory, int passedTestcases, int totalTestcases) {}
