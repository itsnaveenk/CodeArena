package com.codearena.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codearena.document.Submission;
import com.codearena.dto.Judge0Status;
import com.codearena.dto.Judge0Submission;
import com.codearena.dto.RunRequest;
import com.codearena.dto.RunResponse;
import com.codearena.dto.SubmissionResponse;
import com.codearena.dto.SubmitRequest;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.execution.ExecutionGateway;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.util.DefaultOutputNormalizer;
import com.codearena.util.OutputNormalizer;
import com.codearena.util.TokenBucketRateLimiter;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

@Tag("Feature: codearena-platform")
class ExecutionServiceProperties {

    @Property(tries = 100)
    @Tag("Property 20: Run Non-Persistence")
    void runNonPersistence(@ForAll("validRunRequests") RunRequest request, @ForAll("users") User user) {
        ExecutionGateway executionGateway = mock(ExecutionGateway.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService testcaseService = mock(TestcaseService.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        OutputNormalizer outputNormalizer = new DefaultOutputNormalizer();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(
            executionGateway, problemRepository, testcaseService, submissionRepository, outputNormalizer, rateLimiter);

        Problem problem = new Problem();
        problem.setId(request.problemId());
        problem.setStatus(ProblemStatus.PUBLISHED);
        when(problemRepository.findById(request.problemId())).thenReturn(Optional.of(problem));

        Testcase testcase = new Testcase();
        testcase.setInput("test input");
        testcase.setExpectedOutput("test output");
        when(testcaseService.getVisibleTestcases(request.problemId())).thenReturn(List.of(testcase));

        Judge0Submission judge0Response = new Judge0Submission("token", "output", null, null, null,
            new Judge0Status(3, "Accepted"), 0.5, 1024);
        when(executionGateway.execute(any(), any())).thenReturn(judge0Response);

        RunResponse response = executionService.runCode(request, user);

        verify(submissionRepository, never()).save(any(Submission.class));
        assertThat(response).isNotNull();
    }


    @Property(tries = 50)
    @Tag("Property 21: Verdict Determination Correctness")
    void verdictDeterminationCorrectness_Accepted(@ForAll("validSubmitRequests") SubmitRequest request, @ForAll("users") User user) {
        ExecutionGateway executionGateway = mock(ExecutionGateway.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService testcaseService = mock(TestcaseService.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        OutputNormalizer outputNormalizer = new DefaultOutputNormalizer();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(
            executionGateway, problemRepository, testcaseService, submissionRepository, outputNormalizer, rateLimiter);

        Problem problem = new Problem();
        problem.setId(request.problemId());
        problem.setStatus(ProblemStatus.PUBLISHED);
        when(problemRepository.findById(request.problemId())).thenReturn(Optional.of(problem));

        Testcase tc1 = new Testcase();
        tc1.setInput("input1");
        tc1.setExpectedOutput("output1");
        when(testcaseService.getAllTestcasesForExecution(request.problemId())).thenReturn(List.of(tc1));

        Judge0Submission successResponse = new Judge0Submission("token", "output1", null, null, null,
            new Judge0Status(3, "Accepted"), 0.5, 1024);
        when(executionGateway.execute(any(), any())).thenReturn(successResponse);

        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId("test-id");
            return s;
        });

        SubmissionResponse response = executionService.submitCode(request, user);

        assertThat(response.verdict()).isEqualTo(Verdict.ACCEPTED);
        assertThat(response.passedTestcases()).isEqualTo(1);
    }

    @Property(tries = 50)
    @Tag("Property 21a: Compilation Error Verdict")
    void verdictDeterminationCorrectness_CompilationError(@ForAll("validSubmitRequests") SubmitRequest request, @ForAll("users") User user) {
        ExecutionGateway executionGateway = mock(ExecutionGateway.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService testcaseService = mock(TestcaseService.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        OutputNormalizer outputNormalizer = new DefaultOutputNormalizer();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(
            executionGateway, problemRepository, testcaseService, submissionRepository, outputNormalizer, rateLimiter);

        Problem problem = new Problem();
        problem.setId(request.problemId());
        problem.setStatus(ProblemStatus.PUBLISHED);
        when(problemRepository.findById(request.problemId())).thenReturn(Optional.of(problem));

        Testcase tc = new Testcase();
        tc.setInput("input");
        tc.setExpectedOutput("output");
        when(testcaseService.getAllTestcasesForExecution(request.problemId())).thenReturn(List.of(tc));

        Judge0Submission compileError = new Judge0Submission("token", null, null, "error", null,
            new Judge0Status(6, "Compilation Error"), null, null);
        when(executionGateway.execute(any(), any())).thenReturn(compileError);

        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId("test-id");
            return s;
        });

        SubmissionResponse response = executionService.submitCode(request, user);

        assertThat(response.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
    }

    @Property(tries = 50)
    @Tag("Property 21b: TLE Verdict")
    void verdictDeterminationCorrectness_TLE(@ForAll("validSubmitRequests") SubmitRequest request, @ForAll("users") User user) {
        ExecutionGateway executionGateway = mock(ExecutionGateway.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService testcaseService = mock(TestcaseService.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        OutputNormalizer outputNormalizer = new DefaultOutputNormalizer();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(
            executionGateway, problemRepository, testcaseService, submissionRepository, outputNormalizer, rateLimiter);

        Problem problem = new Problem();
        problem.setId(request.problemId());
        problem.setStatus(ProblemStatus.PUBLISHED);
        when(problemRepository.findById(request.problemId())).thenReturn(Optional.of(problem));

        Testcase tc = new Testcase();
        tc.setInput("input");
        tc.setExpectedOutput("output");
        when(testcaseService.getAllTestcasesForExecution(request.problemId())).thenReturn(List.of(tc));

        Judge0Submission tleResponse = new Judge0Submission("token", null, null, null, null,
            new Judge0Status(5, "Time Limit Exceeded"), 5.0, 2048);
        when(executionGateway.execute(any(), any())).thenReturn(tleResponse);

        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId("test-id");
            return s;
        });

        SubmissionResponse response = executionService.submitCode(request, user);

        assertThat(response.verdict()).isEqualTo(Verdict.TLE);
    }

    @Property(tries = 50)
    @Tag("Property 21c: Runtime Error Verdict")
    void verdictDeterminationCorrectness_RuntimeError(@ForAll("validSubmitRequests") SubmitRequest request, @ForAll("users") User user) {
        ExecutionGateway executionGateway = mock(ExecutionGateway.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService testcaseService = mock(TestcaseService.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        OutputNormalizer outputNormalizer = new DefaultOutputNormalizer();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(
            executionGateway, problemRepository, testcaseService, submissionRepository, outputNormalizer, rateLimiter);

        Problem problem = new Problem();
        problem.setId(request.problemId());
        problem.setStatus(ProblemStatus.PUBLISHED);
        when(problemRepository.findById(request.problemId())).thenReturn(Optional.of(problem));

        Testcase tc = new Testcase();
        tc.setInput("input");
        tc.setExpectedOutput("output");
        when(testcaseService.getAllTestcasesForExecution(request.problemId())).thenReturn(List.of(tc));

        Judge0Submission runtimeError = new Judge0Submission("token", null, "NPE", null, null,
            new Judge0Status(11, "Runtime Error"), 0.1, 512);
        when(executionGateway.execute(any(), any())).thenReturn(runtimeError);

        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId("test-id");
            return s;
        });

        SubmissionResponse response = executionService.submitCode(request, user);

        assertThat(response.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
    }

    @Property(tries = 50)
    @Tag("Property 21d: Wrong Answer Verdict")
    void verdictDeterminationCorrectness_WrongAnswer(@ForAll("validSubmitRequests") SubmitRequest request, @ForAll("users") User user) {
        ExecutionGateway executionGateway = mock(ExecutionGateway.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService testcaseService = mock(TestcaseService.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        OutputNormalizer outputNormalizer = new DefaultOutputNormalizer();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(
            executionGateway, problemRepository, testcaseService, submissionRepository, outputNormalizer, rateLimiter);

        Problem problem = new Problem();
        problem.setId(request.problemId());
        problem.setStatus(ProblemStatus.PUBLISHED);
        when(problemRepository.findById(request.problemId())).thenReturn(Optional.of(problem));

        Testcase tc = new Testcase();
        tc.setInput("input");
        tc.setExpectedOutput("expected");
        when(testcaseService.getAllTestcasesForExecution(request.problemId())).thenReturn(List.of(tc));

        Judge0Submission wrongAnswer = new Judge0Submission("token", "wrong", null, null, null,
            new Judge0Status(3, "Accepted"), 0.5, 1024);
        when(executionGateway.execute(any(), any())).thenReturn(wrongAnswer);

        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId("test-id");
            return s;
        });

        SubmissionResponse response = executionService.submitCode(request, user);

        assertThat(response.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
        assertThat(response.passedTestcases()).isEqualTo(0);
    }

    @Provide
    Arbitrary<RunRequest> validRunRequests() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 1000),
            Arbitraries.of(62, 71, 54, 63, 4, 74, 60, 73),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100)
        ).as((problemId, languageId, code) -> new RunRequest(problemId, languageId, code, null));
    }

    @Provide
    Arbitrary<SubmitRequest> validSubmitRequests() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 1000),
            Arbitraries.of(62, 71, 54, 63, 4, 74, 60, 73),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100)
        ).as(SubmitRequest::new);
    }

    @Provide
    Arbitrary<User> users() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 10000),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(10).map(s -> s.toLowerCase() + "@test.com")
        ).as((id, name, email) -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setRole(Role.USER);
            return user;
        });
    }
}
