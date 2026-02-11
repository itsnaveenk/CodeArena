package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.ContestEditorial;

public record ContestEditorialDto(
    Long id,
    Long contestId,
    Long problemId,
    String problemTitle,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ContestEditorialDto fromEntity(ContestEditorial editorial) {
        return new ContestEditorialDto(
            editorial.getId(),
            editorial.getContest().getId(),
            editorial.getProblem().getId(),
            editorial.getProblem().getTitle(),
            editorial.getContent(),
            editorial.getCreatedAt(),
            editorial.getUpdatedAt()
        );
    }
}
