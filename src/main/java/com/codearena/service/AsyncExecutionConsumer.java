package com.codearena.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.codearena.document.Submission;
import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Submission;
import com.codearena.dto.RunResponse;
import com.codearena.dto.SubmissionResponse;
import com.codearena.dto.SubmissionResult;
import com.codearena.dto.SubmissionTask;
import com.codearena.entity.Testcase;
import com.codearena.entity.Verdict;
import com.codearena.execution.ExecutionGateway;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.util.OutputNormalizer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncExecutionConsumer {

    private static final String STREAM_KEY = "codearena:execution:tasks";
    private static final String CONSUMER_GROUP = "execution-workers";
    private static final String CONSUMER_NAME = "worker-1";
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutionGateway executionGateway;
    private final TestcaseService testcaseService;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final OutputNormalizer outputNormalizer;
    private final AsyncExecutionProducer asyncExecutionProducer;

    @PostConstruct
    public void initializeConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, CONSUMER_GROUP);
            log.info("Created consumer group: {}", CONSUMER_GROUP);
        } catch (Exception e) {
            log.info("Consumer group {} already exists", CONSUMER_GROUP);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async("executionTaskExecutor")
    public void consumeTasks() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records != null && !records.isEmpty()) {
                for (MapRecord<String, Object, Object> record : records) {
                    try {
                        SubmissionTask task = (SubmissionTask) record.getValue().get("task");
                        processTask(task);
                        redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());
                        log.info("Processed task {} successfully", task.getTaskId());
                    } catch (Exception e) {
                        log.error("Error processing task: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error consuming tasks from stream: {}", e.getMessage(), e);
        }
    }

    private void processTask(SubmissionTask task) {
        try {
            updateTaskStatus(task.getTaskId(), SubmissionResult.Status.PROCESSING);

            SubmissionResult result;
            if (task.getType() == SubmissionTask.TaskType.RUN) {
                result = processRunTask(task);
            } else {
                result = processSubmitTask(task);
            }

            result.setTaskId(task.getTaskId());
            result.setStatus(SubmissionResult.Status.SUCCESS);
            result.setCompletedAt(LocalDateTime.now());
            asyncExecutionProducer.storeResult(result);

        } catch (Exception e) {
            SubmissionResult errorResult = SubmissionResult.builder()
                    .taskId(task.getTaskId())
                    .status(SubmissionResult.Status.FAILED)
                    .errorMessage(e.getMessage())
                    .createdAt(task.getCreatedAt())
                    .completedAt(LocalDateTime.now())
                    .build();
            asyncExecutionProducer.storeResult(errorResult);

            log.error("Task {} failed: {}", task.getTaskId(), e.getMessage(), e);
        }
    }

    private SubmissionResult processRunTask(SubmissionTask task) {
        Judge0Request request = new Judge0Request(
                task.getLanguageId(),
                task.getCode(),
                task.getInput());

        Judge0Submission judgeResult = executionGateway.execute(request, EXECUTION_TIMEOUT);
        RunResponse runResponse = mapToRunResponse(judgeResult, task.getExpectedOutput());

        return SubmissionResult.builder()
                .runResult(runResponse)
                .build();
    }

    private SubmissionResult processSubmitTask(SubmissionTask task) {
        problemRepository.findById(task.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        List<Testcase> testcases = testcaseService.getAllTestcasesForExecution(task.getProblemId());
        if (testcases.isEmpty()) {
            throw new RuntimeException("No testcases available for evaluation");
        }

        int passedCount = 0;
        Verdict verdict = Verdict.ACCEPTED;
        Double maxRuntime = 0.0;
        Integer maxMemory = 0;

        for (Testcase testcase : testcases) {
            Judge0Request request = new Judge0Request(
                    task.getLanguageId(),
                    task.getCode(),
                    testcase.getInput());

            Judge0Submission judgeResult = executionGateway.execute(request, EXECUTION_TIMEOUT);

            if (judgeResult.time() != null && judgeResult.time() > maxRuntime) {
                maxRuntime = judgeResult.time();
            }
            if (judgeResult.memory() != null && judgeResult.memory() > maxMemory) {
                maxMemory = judgeResult.memory();
            }

            if (judgeResult.isCompilationError()) {
                verdict = Verdict.COMPILATION_ERROR;
                break;
            } else if (judgeResult.isTimeLimitExceeded()) {
                verdict = Verdict.TLE;
                break;
            } else if (judgeResult.isRuntimeError()) {
                verdict = Verdict.RUNTIME_ERROR;
                break;
            } else if (judgeResult.isAccepted()) {
                if (outputNormalizer.areEqual(judgeResult.stdout(), testcase.getExpectedOutput())) {
                    passedCount++;
                } else {
                    verdict = Verdict.WRONG_ANSWER;
                    break;
                }
            } else {
                verdict = Verdict.RUNTIME_ERROR;
                break;
            }
        }

        Submission submission = new Submission();
        submission.setTaskId(task.getTaskId());
        submission.setUserId(task.getUserId());
        submission.setProblemId(task.getProblemId());
        submission.setLanguageId(task.getLanguageId());
        submission.setCode(task.getCode());
        submission.setVerdict(verdict);
        submission.setRuntime(maxRuntime);
        submission.setMemory(maxMemory);
        submission.setPassedTestcases(passedCount);
        submission.setTotalTestcases(testcases.size());

        Submission saved = submissionRepository.save(submission);

        SubmissionResponse submitResponse = new SubmissionResponse(
                saved.getId(),
                verdict,
                maxRuntime,
                maxMemory,
                passedCount,
                testcases.size());

        return SubmissionResult.builder()
                .submitResult(submitResponse)
                .build();
    }

    private RunResponse mapToRunResponse(Judge0Submission result, String expectedOutput) {
        if (result.isCompilationError()) {
            return RunResponse.compilationError(result.compileOutput());
        } else if (result.isTimeLimitExceeded()) {
            return RunResponse.timeLimitExceeded(result.time(), result.memory());
        } else if (result.isRuntimeError()) {
            return RunResponse.runtimeError(result.stderr(), result.time(), result.memory());
        } else {
            if (expectedOutput != null) {
                String actualOutput = normalizeOutput(result.stdout());
                String normalizedExpected = normalizeOutput(expectedOutput);

                if (actualOutput.equals(normalizedExpected)) {
                    return RunResponse.accepted(result.stdout(), result.time(), result.memory(), expectedOutput);
                } else {
                    return RunResponse.wrongAnswer(result.stdout(), result.time(), result.memory(), expectedOutput);
                }
            } else {
                return RunResponse.success(result.stdout(), result.time(), result.memory());
            }
        }
    }

    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        return output.trim().replaceAll("\\r\\n", "\\n");
    }

    private void updateTaskStatus(String taskId, SubmissionResult.Status status) {
        SubmissionResult result = asyncExecutionProducer.getResult(taskId);
        if (result != null) {
            result.setStatus(status);
            asyncExecutionProducer.storeResult(result);
        }
    }
}
