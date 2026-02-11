package com.codearena.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codearena.dto.RunRequest;
import com.codearena.dto.SubmitRequest;
import com.codearena.execution.ExecutionGateway;
import com.codearena.entity.User;
import com.codearena.exception.ValidationException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.util.OutputNormalizer;
import com.codearena.util.RateLimiter;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplValidationTest {

    @Mock private ExecutionGateway executionGateway;
    @Mock private ProblemRepository problemRepository;
    @Mock private TestcaseService testcaseService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private OutputNormalizer outputNormalizer;
    @Mock private RateLimiter rateLimiter;

    @Test
    void submitCode_shouldRejectBlankCodeBeforeCallingJudge0() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any()))
            .thenReturn(true);

        ExecutionServiceImpl service = new ExecutionServiceImpl(
            executionGateway,
            problemRepository,
            testcaseService,
            submissionRepository,
            outputNormalizer,
            rateLimiter
        );

        User user = new User();
        user.setId(1L);

        SubmitRequest request = new SubmitRequest(1L, 62, "   ");

        assertThatThrownBy(() -> service.submitCode(request, user))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid execution request");

        verifyNoInteractions(executionGateway);
        verifyNoInteractions(problemRepository);
        verifyNoInteractions(testcaseService);
        verifyNoInteractions(submissionRepository);
    }

    @Test
    void runCode_shouldRejectNullLanguageIdBeforeCallingJudge0() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any()))
            .thenReturn(true);

        ExecutionServiceImpl service = new ExecutionServiceImpl(
            executionGateway,
            problemRepository,
            testcaseService,
            submissionRepository,
            outputNormalizer,
            rateLimiter
        );

        User user = new User();
        user.setId(1L);

        RunRequest request = new RunRequest(1L, null, "print('x')", null);

        assertThatThrownBy(() -> service.runCode(request, user))
            .isInstanceOf(ValidationException.class);

        verifyNoInteractions(executionGateway);
    }
}
