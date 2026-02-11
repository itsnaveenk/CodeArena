package com.codearena.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionTask {

    public enum TaskType {
        RUN, // Run code against sample test case
        SUBMIT // Submit solution for full evaluation
    }

    private String taskId; // UUID for tracking
    private TaskType type; // RUN or SUBMIT

    private Long userId;
    private Long problemId;
    private Long contestId; // null for non-contest submissions

    private String code;
    private Integer languageId;

    private String input;
    private String expectedOutput;

    private LocalDateTime createdAt;
}
