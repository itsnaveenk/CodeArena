package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.document.ContestDraft;

public record ContestDraftDto(
    Long contestId,
    Long problemId,
    String code,
    Integer languageId,
    LocalDateTime savedAt
) {
    public static ContestDraftDto fromDocument(ContestDraft draft) {
        return new ContestDraftDto(
            draft.getContestId(),
            draft.getProblemId(),
            draft.getCode(),
            draft.getLanguageId(),
            draft.getSavedAt()
        );
    }
}
