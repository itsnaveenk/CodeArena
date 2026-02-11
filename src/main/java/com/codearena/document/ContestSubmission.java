package com.codearena.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codearena.entity.Verdict;

@Document(collection = "contest_submissions")
public class ContestSubmission {

    @Id
    private String id;

    @Indexed
    private Long contestId;

    @Indexed
    private Long problemId;

    @Indexed
    private Long userId;

    @Indexed(unique = true, sparse = true)
    private String taskId;

    private String code;

    private Integer languageId;

    private Verdict verdict;

    private Double runtime;

    private Integer memory;

    private Integer passedTestcases;

    private Integer totalTestcases;

    private Double pointsEarned;

    private Boolean isAutoSubmitted = false;

    @Indexed
    private LocalDateTime submittedAt = LocalDateTime.now();

    public ContestSubmission() {
    }

    public ContestSubmission(Long contestId, Long problemId, Long userId,
            String code, Integer languageId) {
        this.contestId = contestId;
        this.problemId = problemId;
        this.userId = userId;
        this.code = code;
        this.languageId = languageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getContestId() {
        return contestId;
    }

    public void setContestId(Long contestId) {
        this.contestId = contestId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Double getRuntime() {
        return runtime;
    }

    public void setRuntime(Double runtime) {
        this.runtime = runtime;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getPassedTestcases() {
        return passedTestcases;
    }

    public void setPassedTestcases(Integer passedTestcases) {
        this.passedTestcases = passedTestcases;
    }

    public Integer getTotalTestcases() {
        return totalTestcases;
    }

    public void setTotalTestcases(Integer totalTestcases) {
        this.totalTestcases = totalTestcases;
    }

    public Double getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(Double pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public Boolean getIsAutoSubmitted() {
        return isAutoSubmitted;
    }

    public void setIsAutoSubmitted(Boolean isAutoSubmitted) {
        this.isAutoSubmitted = isAutoSubmitted;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    @Override
    public String toString() {
        return "ContestSubmission{id='" + id + "', contestId=" + contestId +
                ", problemId=" + problemId + ", userId=" + userId +
                ", verdict=" + verdict + ", pointsEarned=" + pointsEarned +
                ", submittedAt=" + submittedAt + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContestSubmission that = (ContestSubmission) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
