package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.Verdict;

public record SubmissionDto(
    String id,
    Long userId,
    Long problemId,
    String problemTitle,
    Integer languageId,
    String code,
    Verdict verdict,
    Double runtime,
    Integer memory,
    int passedTestcases,
    int totalTestcases,
    LocalDateTime createdAt
) {}
