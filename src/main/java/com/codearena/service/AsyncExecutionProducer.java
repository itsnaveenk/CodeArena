package com.codearena.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.codearena.dto.SubmissionResult;
import com.codearena.dto.SubmissionTask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncExecutionProducer {

    private static final String STREAM_KEY = "codearena:execution:tasks";
    private static final String RESULT_KEY_PREFIX = "codearena:execution:result:";

    private final RedisTemplate<String, Object> redisTemplate;

    public String submitTask(SubmissionTask task) {
        String taskId = UUID.randomUUID().toString();
        task.setTaskId(taskId);
        task.setCreatedAt(LocalDateTime.now());

        SubmissionResult initialResult = SubmissionResult.builder()
                .taskId(taskId)
                .status(SubmissionResult.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        String resultKey = RESULT_KEY_PREFIX + taskId;
        redisTemplate.opsForValue().set(resultKey, initialResult, 5, TimeUnit.MINUTES);

        java.util.Map<String, Object> taskMap = new java.util.HashMap<>();
        taskMap.put("task", task);
        redisTemplate.opsForStream().add(STREAM_KEY, taskMap);

        log.info("Submitted task {} to execution queue (type: {}, userId: {}, problemId: {})",
                taskId, task.getType(), task.getUserId(), task.getProblemId());

        return taskId;
    }

    @SuppressWarnings("unchecked")
    public SubmissionResult getResult(String taskId) {
        String resultKey = RESULT_KEY_PREFIX + taskId;
        return (SubmissionResult) redisTemplate.opsForValue().get(resultKey);
    }

    public void storeResult(SubmissionResult result) {
        String resultKey = RESULT_KEY_PREFIX + result.getTaskId();
        redisTemplate.opsForValue().set(resultKey, result, 5, TimeUnit.MINUTES);
    }
}
