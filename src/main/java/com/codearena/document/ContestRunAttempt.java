package com.codearena.document;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contest_run_attempts")
@CompoundIndex(name = "idx_run_contest_user_problem", def = "{'contestId': 1, 'userId': 1, 'problemId': 1}")
@CompoundIndex(name = "idx_run_contest_user_time", def = "{'contestId': 1, 'userId': 1, 'createdAt': -1}")
public class ContestRunAttempt {

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

    private List<TestcaseRunResult> results;

    private int passedCount;

    private int totalCount;

    private Double overallRuntime;

    private Integer overallMemory;

    @Indexed(expireAfter = "24h") // Auto-delete after 24 hours
    private LocalDateTime createdAt = LocalDateTime.now();

    public ContestRunAttempt() {
    }

    public ContestRunAttempt(Long contestId, Long problemId, Long userId,
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

    public List<TestcaseRunResult> getResults() {
        return results;
    }

    public void setResults(List<TestcaseRunResult> results) {
        this.results = results;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public Double getOverallRuntime() {
        return overallRuntime;
    }

    public void setOverallRuntime(Double overallRuntime) {
        this.overallRuntime = overallRuntime;
    }

    public Integer getOverallMemory() {
        return overallMemory;
    }

    public void setOverallMemory(Integer overallMemory) {
        this.overallMemory = overallMemory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class TestcaseRunResult {
        private Long testcaseId;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;
        private Double runtime;
        private Integer memory;
        private String status; // ACCEPTED, WRONG_ANSWER, TLE, RUNTIME_ERROR, COMPILATION_ERROR

        public TestcaseRunResult() {
        }

        public TestcaseRunResult(Long testcaseId, String input, String expectedOutput,
                                 String actualOutput, boolean passed, Double runtime,
                                 Integer memory, String status) {
            this.testcaseId = testcaseId;
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.passed = passed;
            this.runtime = runtime;
            this.memory = memory;
            this.status = status;
        }

        public Long getTestcaseId() { return testcaseId; }
        public void setTestcaseId(Long testcaseId) { this.testcaseId = testcaseId; }
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
        public String getActualOutput() { return actualOutput; }
        public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public Double getRuntime() { return runtime; }
        public void setRuntime(Double runtime) { this.runtime = runtime; }
        public Integer getMemory() { return memory; }
        public void setMemory(Integer memory) { this.memory = memory; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
