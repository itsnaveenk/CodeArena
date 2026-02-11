package com.codearena.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codearena.entity.Verdict;

@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private Long problemId;

    @Indexed(unique = true, sparse = true)
    private String taskId;

    private Integer languageId;

    private String code;

    private Verdict verdict;

    private Double runtime;

    private Integer memory;

    private int passedTestcases;

    private int totalTestcases;

    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();

    public Submission() {
    }

    public Submission(Long userId, Long problemId, Integer languageId, String code,
            Verdict verdict, Double runtime, Integer memory,
            int passedTestcases, int totalTestcases) {
        this.userId = userId;
        this.problemId = problemId;
        this.languageId = languageId;
        this.code = code;
        this.verdict = verdict;
        this.runtime = runtime;
        this.memory = memory;
        this.passedTestcases = passedTestcases;
        this.totalTestcases = totalTestcases;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
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

    public int getPassedTestcases() {
        return passedTestcases;
    }

    public void setPassedTestcases(int passedTestcases) {
        this.passedTestcases = passedTestcases;
    }

    public int getTotalTestcases() {
        return totalTestcases;
    }

    public void setTotalTestcases(int totalTestcases) {
        this.totalTestcases = totalTestcases;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Submission{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", problemId=" + problemId +
                ", languageId=" + languageId +
                ", verdict=" + verdict +
                ", runtime=" + runtime +
                ", memory=" + memory +
                ", passedTestcases=" + passedTestcases +
                ", totalTestcases=" + totalTestcases +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Submission that = (Submission) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
