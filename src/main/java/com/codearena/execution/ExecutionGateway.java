package com.codearena.execution;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codearena.config.ExecutionConfig;
import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Submission;
import com.codearena.exception.Judge0Exception;
import com.codearena.integration.Judge0Client;

@Component
public class ExecutionGateway {

    private static final Logger log = LoggerFactory.getLogger(ExecutionGateway.class);

    private final ExecutionConfig executionConfig;
    private final Judge0Client judge0Client;
    private final LocalToolchainExecutor localToolchainExecutor;

    public ExecutionGateway(
            ExecutionConfig executionConfig,
            Judge0Client judge0Client,
            LocalToolchainExecutor localToolchainExecutor) {
        this.executionConfig = executionConfig;
        this.judge0Client = judge0Client;
        this.localToolchainExecutor = localToolchainExecutor;
    }

    public Judge0Submission execute(Judge0Request request, Duration timeout) {
        ExecutionBackend backend = ExecutionBackend.fromConfig(executionConfig.getBackend());

        return switch (backend) {
            case LOCAL -> localToolchainExecutor.execute(request.languageId(), request.sourceCode(), request.stdin(), timeout);
            case JUDGE0 -> executeJudge0WithOptionalFallback(request, timeout);
        };
    }

    private Judge0Submission executeJudge0WithOptionalFallback(Judge0Request request, Duration timeout) {
        try {
            return judge0Client.createAndWaitForResult(request, timeout);
        } catch (Judge0Exception e) {
            if (!executionConfig.isFallbackToLocal()) {
                throw e;
            }
            log.warn("Judge0 unavailable ({}). Falling back to local execution.", e.getMessage());
            return localToolchainExecutor.execute(request.languageId(), request.sourceCode(), request.stdin(), timeout);
        }
    }
}
