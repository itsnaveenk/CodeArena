package com.codearena.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.RunRequest;
import com.codearena.dto.RunResponse;
import com.codearena.dto.SubmissionResponse;
import com.codearena.dto.SubmissionResult;
import com.codearena.dto.SubmissionTask;
import com.codearena.dto.SubmitRequest;
import com.codearena.entity.User;
import com.codearena.service.AsyncExecutionProducer;
import com.codearena.service.ExecutionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/execute")
public class ExecutionController {

    private final ExecutionService executionService;
    private final AsyncExecutionProducer asyncExecutionProducer;

    public ExecutionController(ExecutionService executionService, AsyncExecutionProducer asyncExecutionProducer) {
        this.executionService = executionService;
        this.asyncExecutionProducer = asyncExecutionProducer;
    }

    @Deprecated
    @PostMapping("/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RunResponse> runCode(
            @Valid @RequestBody RunRequest request,
            @AuthenticationPrincipal User user) {
        RunResponse response = executionService.runCode(request, user);
        return ResponseEntity.ok(response);
    }

    @Deprecated
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubmissionResponse> submitCode(
            @Valid @RequestBody SubmitRequest request,
            @AuthenticationPrincipal User user) {
        SubmissionResponse response = executionService.submitCode(request, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/run-async")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> runCodeAsync(
            @Valid @RequestBody RunRequest request,
            @AuthenticationPrincipal User user) {

        SubmissionTask task = SubmissionTask.builder()
                .type(SubmissionTask.TaskType.RUN)
                .userId(user.getId())
                .problemId(request.problemId())
                .code(request.code())
                .languageId(request.languageId())
                .input(request.customInput() != null ? request.customInput() : "")
                .build();

        String taskId = asyncExecutionProducer.submitTask(task);

        return ResponseEntity.accepted()
                .body(Map.of("taskId", taskId));
    }

    @PostMapping("/submit-async")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> submitCodeAsync(
            @Valid @RequestBody SubmitRequest request,
            @AuthenticationPrincipal User user) {

        SubmissionTask task = SubmissionTask.builder()
                .type(SubmissionTask.TaskType.SUBMIT)
                .userId(user.getId())
                .problemId(request.problemId())
                .code(request.code())
                .languageId(request.languageId())
                .build();

        String taskId = asyncExecutionProducer.submitTask(task);

        return ResponseEntity.accepted()
                .body(Map.of("taskId", taskId));
    }

    @GetMapping("/result/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getResult(@PathVariable String taskId) {
        SubmissionResult result = asyncExecutionProducer.getResult(taskId);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found or expired"));
        }

        return ResponseEntity.ok(result);
    }
}
