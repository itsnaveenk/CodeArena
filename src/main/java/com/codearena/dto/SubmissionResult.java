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
public class SubmissionResult {

    public enum Status {
        PENDING, // Queued, not yet processed
        PROCESSING, // Currently executing
        SUCCESS, // Completed successfully
        FAILED // Execution error or timeout
    }

    private String taskId;

    private Status status;

    private RunResponse runResult; // For RUN tasks
    private SubmissionResponse submitResult; // For SUBMIT tasks

    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
