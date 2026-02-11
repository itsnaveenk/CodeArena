package com.codearena.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contest_drafts")
@CompoundIndex(name = "uk_contest_problem_user_draft", def = "{'contestId': 1, 'problemId': 1, 'userId': 1}", unique = true)
public class ContestDraft {

    @Id
    private String id;

    @Indexed
    private Long contestId;

    @Indexed
    private Long problemId;

    @Indexed
    private Long userId;

    private String code;

    private Integer languageId;

    private LocalDateTime savedAt = LocalDateTime.now();

    public ContestDraft() {
    }

    public ContestDraft(Long contestId, Long problemId, Long userId, String code, Integer languageId) {
        this.contestId = contestId;
        this.problemId = problemId;
        this.userId = userId;
        this.code = code;
        this.languageId = languageId;
        this.savedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getContestId() { return contestId; }
    public void setContestId(Long contestId) { this.contestId = contestId; }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getLanguageId() { return languageId; }
    public void setLanguageId(Integer languageId) { this.languageId = languageId; }

    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }

    @Override
    public String toString() {
        return "ContestDraft{id='" + id + "', contestId=" + contestId +
               ", problemId=" + problemId + ", userId=" + userId +
               ", languageId=" + languageId + ", savedAt=" + savedAt + '}';
    }
}
