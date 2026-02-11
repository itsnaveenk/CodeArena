package com.codearena.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.document.ContestDraft;
import com.codearena.document.ContestRunAttempt;
import com.codearena.document.ContestSubmission;
import com.codearena.dto.ContestDraftDto;
import com.codearena.dto.ContestRunResponse;
import com.codearena.dto.ContestSubmissionDto;
import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Submission;
import com.codearena.dto.ParticipationStatus;
import com.codearena.dto.RunRequest;
import com.codearena.dto.SubmitRequest;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ScoringModel;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.ContestSubmissionException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.execution.ExecutionGateway;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRegistrationRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ContestRunAttemptRepository;
import com.codearena.repository.ContestSubmissionRepository;
import com.codearena.util.OutputNormalizer;

@Service
@Transactional
public class ContestSubmissionServiceImpl implements ContestSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(ContestSubmissionServiceImpl.class);

    private static final int SUBMIT_RATE_LIMIT = 5;

    private static final int RUN_RATE_LIMIT = 10;
    
    /** Execution timeout for code execution */
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(30);

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final ContestSubmissionRepository submissionRepository;
    private final ContestRunAttemptRepository runAttemptRepository;
    private final ContestRegistrationService registrationService;
    private final TestcaseService testcaseService;
    private final ExecutionGateway executionGateway;
    private final OutputNormalizer outputNormalizer;
    private final ContestLeaderboardService leaderboardService;
    private final com.codearena.repository.ContestDraftRepository draftRepository;

    public ContestSubmissionServiceImpl(
            ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository,
            ContestRegistrationRepository registrationRepository,
            ContestSubmissionRepository submissionRepository,
            ContestRunAttemptRepository runAttemptRepository,
            ContestRegistrationService registrationService,
            TestcaseService testcaseService,
            ExecutionGateway executionGateway,
            OutputNormalizer outputNormalizer,
            ContestLeaderboardService leaderboardService,
            com.codearena.repository.ContestDraftRepository draftRepository) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.registrationRepository = registrationRepository;
        this.submissionRepository = submissionRepository;
        this.runAttemptRepository = runAttemptRepository;
        this.registrationService = registrationService;
        this.testcaseService = testcaseService;
        this.executionGateway = executionGateway;
        this.outputNormalizer = outputNormalizer;
        this.leaderboardService = leaderboardService;
        this.draftRepository = draftRepository;
    }

    // ==================== Run Code Operations ====================

    @Override
    public ContestRunResponse runCode(Long contestId, Long problemId, RunRequest request, User user) {
        log.debug("Processing contest run: contestId={}, problemId={}, userId={}", 
                  contestId, problemId, user.getId());

        Contest contest = findContestOrThrow(contestId);

        // Validate eligibility (same checks as submit)
        validateSubmissionEligibility(contest, user);

        // Verify problem is in contest
        contestProblemRepository.findByContestIdAndProblemId(contestId, problemId)
            .orElseThrow(ContestSubmissionException::problemNotInContest);

        // Rate limit: 10 runs per minute per user per problem
        checkRunRateLimit(contestId, user.getId(), problemId);

        // Get SAMPLE testcases only (is_hidden = false)
        List<Testcase> sampleTestcases = testcaseService.getVisibleTestcases(problemId);
        if (sampleTestcases.isEmpty()) {
            throw new IllegalStateException("No sample testcases available for this problem");
        }

        // Execute against sample testcases and collect per-testcase results
        List<ContestRunResponse.TestcaseResult> results = new ArrayList<>();
        int passedCount = 0;
        double maxRuntime = 0.0;
        int maxMemory = 0;

        for (Testcase tc : sampleTestcases) {
            Judge0Request judge0Request = new Judge0Request(
                request.languageId(), request.code(), tc.getInput());

            Judge0Submission result = executionGateway.execute(judge0Request, EXECUTION_TIMEOUT);

            Double runtime = result.time();
            Integer memory = result.memory();
            if (runtime != null && runtime > maxRuntime) maxRuntime = runtime;
            if (memory != null && memory > maxMemory) maxMemory = memory;

            String actualOutput = result.stdout();
            String status;
            boolean passed = false;

            if (result.isCompilationError()) {
                status = "COMPILATION_ERROR";
                actualOutput = result.compileOutput();
            } else if (result.isTimeLimitExceeded()) {
                status = "TIME_LIMIT_EXCEEDED";
            } else if (result.isRuntimeError()) {
                status = "RUNTIME_ERROR";
                actualOutput = result.stderr();
            } else if (result.isAccepted()) {
                if (outputNormalizer.areEqual(result.stdout(), tc.getExpectedOutput())) {
                    status = "ACCEPTED";
                    passed = true;
                    passedCount++;
                } else {
                    status = "WRONG_ANSWER";
                }
            } else {
                status = "RUNTIME_ERROR";
                actualOutput = result.stderr();
            }

            results.add(new ContestRunResponse.TestcaseResult(
                tc.getId(), tc.getInput(), tc.getExpectedOutput(),
                actualOutput, passed, runtime, memory, status));
        }

        // Store run attempt for debugging (24h TTL via MongoDB index)
        storeRunAttempt(contestId, problemId, user.getId(), request, results, passedCount, sampleTestcases.size(), maxRuntime, maxMemory);

        return new ContestRunResponse(results, passedCount, sampleTestcases.size(), maxRuntime, maxMemory);
    }

    // ==================== Submission Operations ====================

    @Override
    public ContestSubmissionDto submit(Long contestId, Long problemId, SubmitRequest request, User user) {
        log.debug("Processing contest submission: contestId={}, problemId={}, userId={}", 
                  contestId, problemId, user.getId());

        Contest contest = findContestOrThrow(contestId);

        // Requirement 6.1.1: Check eligibility using participation status
        validateSubmissionEligibility(contest, user);

        // Requirement 6.5.1: Check rate limit (5 submissions per minute per problem)
        checkSubmitRateLimit(contestId, user.getId(), problemId);

        // Verify problem is in contest
        ContestProblem contestProblem = contestProblemRepository
            .findByContestIdAndProblemId(contestId, problemId)
            .orElseThrow(ContestSubmissionException::problemNotInContest);

        // Get registration for updating submission times
        ContestRegistration registration = registrationRepository
            .findByContestIdAndUserId(contestId, user.getId())
            .orElseThrow(ContestSubmissionException::notRegistered);

        // Find previous best score BEFORE executing (for response context)
        Optional<ContestSubmission> previousBest = submissionRepository
            .findFirstByContestIdAndUserIdAndProblemIdOrderByPointsEarnedDesc(
                contestId, user.getId(), problemId);
        double previousBestPoints = previousBest.map(ContestSubmission::getPointsEarned).orElse(0.0);

        // Execute code and get results
        ExecutionResult executionResult = executeCode(problemId, request);

        // Calculate points based on scoring model (Requirements 6.3.1, 6.3.2)
        double pointsEarned = calculatePoints(
            contest.getScoringModel(),
            contestProblem.getPointValue(),
            executionResult.verdict(),
            executionResult.passedTestcases(),
            executionResult.totalTestcases()
        );

        // Create and save ContestSubmission (Requirement 6.2.1)
        LocalDateTime now = LocalDateTime.now();
        ContestSubmission submission = new ContestSubmission(
            contestId, problemId, user.getId(),
            request.code(), request.languageId()
        );
        submission.setVerdict(executionResult.verdict());
        submission.setRuntime(executionResult.runtime());
        submission.setMemory(executionResult.memory());
        submission.setPassedTestcases(executionResult.passedTestcases());
        submission.setTotalTestcases(executionResult.totalTestcases());
        submission.setPointsEarned(pointsEarned);
        submission.setIsAutoSubmitted(false);
        submission.setSubmittedAt(now);

        ContestSubmission saved = submissionRepository.save(submission);
        log.info("Contest submission saved: id={}, verdict={}, points={}", 
                 saved.getId(), saved.getVerdict(), saved.getPointsEarned());

        // Update registration submission times (Requirement 5.6.1, 5.6.2)
        updateRegistrationSubmissionTimes(registration, now);

        // Trigger leaderboard update only if score improved (Requirement 6.6.1)
        boolean scoreImproved = pointsEarned > previousBestPoints;
        if (scoreImproved) {
            try {
                leaderboardService.updateUserScore(contestId, user.getId(), saved);
            } catch (Exception e) {
                log.error("Failed to update leaderboard for contestId={}, userId={}: {}", 
                          contestId, user.getId(), e.getMessage());
            }
        } else if (previousBest.isEmpty()) {
            // First submission ever — always update leaderboard even if 0 points
            try {
                leaderboardService.updateUserScore(contestId, user.getId(), saved);
            } catch (Exception e) {
                log.error("Failed to create leaderboard entry for contestId={}, userId={}: {}", 
                          contestId, user.getId(), e.getMessage());
            }
        }

        boolean isBest = pointsEarned >= previousBestPoints;
        return new ContestSubmissionDto(
            saved.getId(), saved.getContestId(), saved.getProblemId(),
            saved.getLanguageId(), saved.getVerdict(), saved.getRuntime(), saved.getMemory(),
            saved.getPassedTestcases() != null ? saved.getPassedTestcases() : 0,
            saved.getTotalTestcases() != null ? saved.getTotalTestcases() : 0,
            pointsEarned, saved.getSubmittedAt(),
            false, isBest, previousBestPoints, scoreImproved
        );
    }

    // ==================== Query Methods ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ContestSubmissionDto> getUserSubmissions(Long contestId, User user, Pageable pageable) {
        // Verify contest exists
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return submissionRepository
            .findByContestIdAndUserId(contestId, user.getId(), pageable)
            .map(ContestSubmissionDto::fromDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContestSubmissionDto> getUserSubmissionsForProblem(Long contestId, Long problemId, 
                                                                    User user, Pageable pageable) {
        // Verify contest exists
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return submissionRepository
            .findByContestIdAndUserIdAndProblemId(contestId, user.getId(), problemId, pageable)
            .map(ContestSubmissionDto::fromDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public ContestSubmissionDto getSubmission(String submissionId, User user) {
        ContestSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        // Requirement 6.4.3: Only allow viewing own submissions during contest
        Contest contest = contestRepository.findById(submission.getContestId())
            .orElseThrow(() -> new ContestNotFoundException(submission.getContestId()));

        // During contest, only allow viewing own submissions
        if (contest.getStatus() == ContestStatus.RUNNING) {
            if (!submission.getUserId().equals(user.getId())) {
                throw new ContestSubmissionException("ACCESS_DENIED", 
                    "Cannot view other participants' submissions during contest");
            }
        }

        return ContestSubmissionDto.fromDocument(submission);
    }

    // ==================== Helper Methods ====================

    /**
     * Validates that the user can submit to the contest.
     * Checks registration status, contest status, and time constraints.
     */
    private void validateSubmissionEligibility(Contest contest, User user) {
        ParticipationStatus status = registrationService.checkParticipationStatus(contest.getId(), user);

        if (!status.canParticipate()) {
            String reason = status.reason();
            if (ParticipationStatus.REASON_NOT_REGISTERED.equals(reason)) {
                throw ContestSubmissionException.notRegistered();
            } else if (ParticipationStatus.REASON_WITHDRAWN.equals(reason)) {
                throw ContestSubmissionException.notRegistered();
            } else if (ParticipationStatus.REASON_CONTEST_NOT_RUNNING.equals(reason) ||
                       ParticipationStatus.REASON_CONTEST_NOT_STARTED.equals(reason) ||
                       ParticipationStatus.REASON_CONTEST_ENDED.equals(reason)) {
                throw ContestSubmissionException.contestNotRunning();
            } else if (ParticipationStatus.REASON_NOT_STARTED.equals(reason)) {
                throw ContestSubmissionException.notStarted();
            } else if (ParticipationStatus.REASON_TIME_EXPIRED.equals(reason)) {
                throw ContestSubmissionException.timeExpired();
            } else {
                throw new ContestSubmissionException("CANNOT_PARTICIPATE", reason);
            }
        }
    }

    private void checkSubmitRateLimit(Long contestId, Long userId, Long problemId) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentSubmissions = submissionRepository
            .countByContestIdAndUserIdAndProblemIdAndSubmittedAtAfter(contestId, userId, problemId, oneMinuteAgo);

        if (recentSubmissions >= SUBMIT_RATE_LIMIT) {
            log.warn("Submit rate limit exceeded for userId={} in contestId={} problemId={}: {} submissions in last minute",
                     userId, contestId, problemId, recentSubmissions);
            throw ContestSubmissionException.rateLimited();
        }
    }

    private void checkRunRateLimit(Long contestId, Long userId, Long problemId) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentRuns = runAttemptRepository
            .countByContestIdAndUserIdAndProblemIdAndCreatedAtAfter(contestId, userId, problemId, oneMinuteAgo);

        if (recentRuns >= RUN_RATE_LIMIT) {
            log.warn("Run rate limit exceeded for userId={} in contestId={} problemId={}: {} runs in last minute",
                     userId, contestId, problemId, recentRuns);
            throw ContestSubmissionException.rateLimited();
        }
    }

    private void storeRunAttempt(Long contestId, Long problemId, Long userId,
                                  RunRequest request, List<ContestRunResponse.TestcaseResult> results,
                                  int passedCount, int totalCount, double maxRuntime, int maxMemory) {
        try {
            ContestRunAttempt attempt = new ContestRunAttempt(contestId, problemId, userId,
                request.code(), request.languageId());
            attempt.setPassedCount(passedCount);
            attempt.setTotalCount(totalCount);
            attempt.setOverallRuntime(maxRuntime);
            attempt.setOverallMemory(maxMemory);

            List<ContestRunAttempt.TestcaseRunResult> storedResults = results.stream()
                .map(r -> new ContestRunAttempt.TestcaseRunResult(
                    r.testcaseId(), r.input(), r.expectedOutput(), r.actualOutput(),
                    r.passed(), r.runtime(), r.memory(), r.status()))
                .toList();
            attempt.setResults(storedResults);

            runAttemptRepository.save(attempt);
            log.debug("Stored run attempt for userId={} contestId={} problemId={}", userId, contestId, problemId);
        } catch (Exception e) {
            log.warn("Failed to store run attempt: {}", e.getMessage());
        }
    }

    private ExecutionResult executeCode(Long problemId, SubmitRequest request) {
        List<Testcase> testcases = testcaseService.getAllTestcasesForExecution(problemId);
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
                }
            } else {
                verdict = Verdict.RUNTIME_ERROR;
                break;
            }
        }

        if (passedCount == testcases.size()) {
            verdict = Verdict.ACCEPTED;
        }

        return new ExecutionResult(verdict, maxRuntime, maxMemory, passedCount, testcases.size());
    }

    private double calculatePoints(ScoringModel scoringModel, int pointValue, 
                                   Verdict verdict, int passedTestcases, int totalTestcases) {
        if (scoringModel == ScoringModel.BINARY) {
            return verdict == Verdict.ACCEPTED ? pointValue : 0.0;
        } else {
            if (totalTestcases == 0) {
                return 0.0;
            }
            double ratio = (double) passedTestcases / totalTestcases;
            double points = pointValue * ratio;
            
            return BigDecimal.valueOf(points)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        }
    }

    private void updateRegistrationSubmissionTimes(ContestRegistration registration, LocalDateTime submissionTime) {
        if (registration.getFirstSubmissionTime() == null) {
            registration.setFirstSubmissionTime(submissionTime);
        }
        registration.setLastSubmissionTime(submissionTime);
        registrationRepository.save(registration);
    }

    private Contest findContestOrThrow(Long contestId) {
        return contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));
    }


    @Override
    public void saveDraft(Long contestId, Long problemId, String code, Integer languageId, User user) {
        log.debug("Saving draft: contestId={}, problemId={}, userId={}", contestId, problemId, user.getId());

        Optional<ContestDraft> existing = draftRepository
            .findByContestIdAndProblemIdAndUserId(contestId, problemId, user.getId());

        if (existing.isPresent()) {
            ContestDraft draft = existing.get();
            draft.setCode(code);
            draft.setLanguageId(languageId);
            draft.setSavedAt(LocalDateTime.now());
            draftRepository.save(draft);
        } else {
            ContestDraft draft = new ContestDraft(contestId, problemId, user.getId(), code, languageId);
            draftRepository.save(draft);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ContestDraftDto getDraft(Long contestId, Long problemId, User user) {
        return draftRepository
            .findByContestIdAndProblemIdAndUserId(contestId, problemId, user.getId())
            .map(ContestDraftDto::fromDocument)
            .orElse(null);
    }


    @Override
    public void autoSubmitForUser(Long contestId, Long userId) {
        log.info("Auto-submitting drafts for userId={} in contestId={}", userId, contestId);

        Contest contest = findContestOrThrow(contestId);
        List<ContestProblem> problems = contestProblemRepository.findByContestIdOrderByDisplayOrderAsc(contestId);
        List<ContestDraft> drafts = draftRepository.findByContestIdAndUserId(contestId, userId);

        int autoSubmitted = 0;
        for (ContestDraft draft : drafts) {
            boolean hasSubmission = submissionRepository.existsByContestIdAndUserIdAndProblemId(
                contestId, userId, draft.getProblemId());

            if (hasSubmission) {
                log.debug("Skipping auto-submit for problemId={} - user already has submissions", draft.getProblemId());
                continue;
            }

            Optional<ContestProblem> contestProblem = problems.stream()
                .filter(cp -> cp.getProblem().getId().equals(draft.getProblemId()))
                .findFirst();

            if (contestProblem.isEmpty()) {
                log.debug("Skipping auto-submit for problemId={} - not in contest", draft.getProblemId());
                continue;
            }

            try {
                SubmitRequest request = new SubmitRequest(draft.getProblemId(), draft.getLanguageId(), draft.getCode());
                ExecutionResult result = executeCode(draft.getProblemId(), request);

                double pointsEarned = calculatePoints(
                    contest.getScoringModel(),
                    contestProblem.get().getPointValue(),
                    result.verdict(), result.passedTestcases(), result.totalTestcases()
                );

                ContestSubmission submission = new ContestSubmission(
                    contestId, draft.getProblemId(), userId,
                    draft.getCode(), draft.getLanguageId()
                );
                submission.setVerdict(result.verdict());
                submission.setRuntime(result.runtime());
                submission.setMemory(result.memory());
                submission.setPassedTestcases(result.passedTestcases());
                submission.setTotalTestcases(result.totalTestcases());
                submission.setPointsEarned(pointsEarned);
                submission.setIsAutoSubmitted(true);
                submission.setSubmittedAt(LocalDateTime.now());

                ContestSubmission saved = submissionRepository.save(submission);
                autoSubmitted++;

                try {
                    leaderboardService.updateUserScore(contestId, userId, saved);
                } catch (Exception e) {
                    log.error("Failed to update leaderboard after auto-submit: {}", e.getMessage());
                }

                log.info("Auto-submitted for userId={}, problemId={}, points={}", 
                         userId, draft.getProblemId(), pointsEarned);
            } catch (Exception e) {
                log.error("Auto-submit failed for userId={}, problemId={}: {}", 
                          userId, draft.getProblemId(), e.getMessage());
            }
        }

        log.info("Auto-submit complete for userId={} in contestId={}: {} submissions", userId, contestId, autoSubmitted);
    }

    @Override
    public void autoSubmitForContest(Long contestId) {
        log.info("Auto-submitting drafts for all participants in contestId={}", contestId);

        List<ContestRegistration> registrations = registrationRepository
            .findByContestIdAndStatus(contestId, 
                com.codearena.entity.RegistrationStatus.REGISTERED, 
                org.springframework.data.domain.Pageable.unpaged())
            .getContent();

        int totalAutoSubmitted = 0;
        for (ContestRegistration reg : registrations) {
            try {
                autoSubmitForUser(contestId, reg.getUser().getId());
                totalAutoSubmitted++;
            } catch (Exception e) {
                log.error("Auto-submit failed for userId={} in contestId={}: {}", 
                          reg.getUser().getId(), contestId, e.getMessage());
            }
        }

        log.info("Contest-wide auto-submit complete for contestId={}: processed {} participants", 
                 contestId, totalAutoSubmitted);
    }

    private record ExecutionResult(
        Verdict verdict,
        Double runtime,
        Integer memory,
        int passedTestcases,
        int totalTestcases
    ) {}
}
