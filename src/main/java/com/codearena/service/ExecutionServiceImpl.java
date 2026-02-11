package com.codearena.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.codearena.document.Submission;
import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Submission;
import com.codearena.dto.RunRequest;
import com.codearena.dto.RunResponse;
import com.codearena.dto.SubmissionResponse;
import com.codearena.dto.SubmitRequest;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.SupportedLanguage;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.exception.RateLimitExceededException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.exception.ValidationException;
import com.codearena.execution.ExecutionGateway;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.util.OutputNormalizer;
import com.codearena.util.RateLimiter;

@Service
public class ExecutionServiceImpl implements ExecutionService {

    private static final int RUN_MAX_REQUESTS = 10;
    private static final int SUBMIT_MAX_REQUESTS = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(30);

    private final ExecutionGateway executionGateway;
    private final ProblemRepository problemRepository;
    private final TestcaseService testcaseService;
    private final SubmissionRepository submissionRepository;
    private final OutputNormalizer outputNormalizer;
    private final RateLimiter rateLimiter;

    public ExecutionServiceImpl(
            ExecutionGateway executionGateway,
            ProblemRepository problemRepository,
            TestcaseService testcaseService,
            SubmissionRepository submissionRepository,
            OutputNormalizer outputNormalizer,
            RateLimiter rateLimiter) {
        this.executionGateway = executionGateway;
        this.problemRepository = problemRepository;
        this.testcaseService = testcaseService;
        this.submissionRepository = submissionRepository;
        this.outputNormalizer = outputNormalizer;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public RunResponse runCode(RunRequest request, User user) {
        checkRateLimit("run", user.getId(), RUN_MAX_REQUESTS);

        validateExecutionRequest(request.problemId(), request.languageId(), request.code());

        validateLanguage(request.languageId());

        Problem problem = problemRepository.findById(request.problemId())
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        if (problem.getStatus() != ProblemStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Problem not found");
        }

        String input;
        String expectedOutput = null;
        
        if (request.customInput() != null && !request.customInput().isEmpty()) {
            input = request.customInput();
        } else {
            List<Testcase> visibleTestcases = testcaseService.getVisibleTestcases(request.problemId());
            if (visibleTestcases.isEmpty()) {
                throw new IllegalStateException("No visible testcases available");
            }
            Testcase testcase = visibleTestcases.get(0);
            input = testcase.getInput();
            expectedOutput = testcase.getExpectedOutput();
        }

        Judge0Request judge0Request = new Judge0Request(
            request.languageId(),
            request.code(),
            input
        );

        Judge0Submission result = executionGateway.execute(judge0Request, EXECUTION_TIMEOUT);

        return mapToRunResponse(result, expectedOutput);
    }

    @Override
    public SubmissionResponse submitCode(SubmitRequest request, User user) {
        checkRateLimit("submit", user.getId(), SUBMIT_MAX_REQUESTS);

        validateExecutionRequest(request.problemId(), request.languageId(), request.code());

        validateLanguage(request.languageId());

        Problem problem = problemRepository.findById(request.problemId())
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        if (problem.getStatus() != ProblemStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Problem not found");
        }

        List<Testcase> testcases = testcaseService.getAllTestcasesForExecution(request.problemId());
        if (testcases.isEmpty()) {
            throw new IllegalStateException("No testcases available for evaluation");
        }

        int passedCount = 0;
        Verdict verdict = Verdict.ACCEPTED;
        Double maxRuntime = 0.0;
        Integer maxMemory = 0;

        for (Testcase testcase : testcases) {
            Judge0Request judge0Request = new Judge0Request(
                request.languageId(),
                request.code(),
                testcase.getInput()
            );

            Judge0Submission result = executionGateway.execute(judge0Request, EXECUTION_TIMEOUT);

            if (result.time() != null && result.time() > maxRuntime) {
                maxRuntime = result.time();
            }
            if (result.memory() != null && result.memory() > maxMemory) {
                maxMemory = result.memory();
            }

            if (result.isCompilationError()) {
                verdict = Verdict.COMPILATION_ERROR;
                break;
            } else if (result.isTimeLimitExceeded()) {
                verdict = Verdict.TLE;
                break;
            } else if (result.isRuntimeError()) {
                verdict = Verdict.RUNTIME_ERROR;
                break;
            } else if (result.isAccepted()) {
                if (outputNormalizer.areEqual(result.stdout(), testcase.getExpectedOutput())) {
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
        submission.setUserId(user.getId());
        submission.setProblemId(request.problemId());
        submission.setLanguageId(request.languageId());
        submission.setCode(request.code());
        submission.setVerdict(verdict);
        submission.setRuntime(maxRuntime);
        submission.setMemory(maxMemory);
        submission.setPassedTestcases(passedCount);
        submission.setTotalTestcases(testcases.size());

        Submission saved = submissionRepository.save(submission);

        return new SubmissionResponse(
            saved.getId(),
            verdict,
            maxRuntime,
            maxMemory,
            passedCount,
            testcases.size()
        );
    }

    private void checkRateLimit(String endpoint, Long userId, int maxRequests) {
        String key = endpoint + ":" + userId;
        if (!rateLimiter.tryAcquire(key, maxRequests, RATE_LIMIT_WINDOW)) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(key);
            throw new RateLimitExceededException(
                "Too many requests. Please try again later.",
                retryAfter
            );
        }
    }

    private void validateLanguage(int languageId) {
        try {
            SupportedLanguage.fromJudge0Id(languageId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported language ID: " + languageId);
        }
    }

    private void validateExecutionRequest(Long problemId, Integer languageId, String code) {
        Map<String, String> details = new HashMap<>();

        if (problemId == null) {
            details.put("problemId", "must not be null");
        }
        if (languageId == null) {
            details.put("languageId", "must not be null");
        }
        if (!StringUtils.hasText(code)) {
            details.put("code", "must not be blank");
        }

        if (!details.isEmpty()) {
            throw new ValidationException("Invalid execution request", details);
        }
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
}
