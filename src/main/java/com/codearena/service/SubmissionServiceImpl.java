package com.codearena.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.codearena.document.Submission;
import com.codearena.dto.SubmissionDto;
import com.codearena.dto.SubmitRequest;
import com.codearena.dto.UserStatsDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository,
                                  ProblemRepository problemRepository) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
    }

    @Override
    public Submission recordSubmission(SubmitRequest request, User user, Verdict verdict,
                                        Double runtime, Integer memory, int passed, int total) {
        Submission submission = new Submission(
            user.getId(),
            request.problemId(),
            request.languageId(),
            request.code(),
            verdict,
            runtime,
            memory,
            passed,
            total
        );
        return submissionRepository.save(submission);
    }

    @Override
    public Page<SubmissionDto> getUserSubmissions(Long userId, Long problemId, 
                                                   Pageable pageable, User requester) {
        if (!requester.getId().equals(userId) && requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You can only view your own submissions");
        }

        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Submission> submissions;
        if (problemId != null) {
            submissions = submissionRepository.findByUserIdAndProblemId(userId, problemId, sortedPageable);
        } else {
            submissions = submissionRepository.findByUserId(userId, sortedPageable);
        }

        return submissions.map(this::toDto);
    }

    @Override
    public SubmissionDto getSubmission(String id, User requester) {
        Submission submission = submissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (!submission.getUserId().equals(requester.getId()) && requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You can only view your own submissions");
        }

        return toDto(submission);
    }

    @Override
    public Page<SubmissionDto> getAllSubmissions(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return submissionRepository.findAll(sortedPageable).map(this::toDto);
    }

    @Override
    public Page<SubmissionDto> getAllSubmissionsFiltered(Verdict verdict, Long userId, Long problemId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Submission> submissions;

        if (verdict != null && userId != null && problemId != null) {
            submissions = submissionRepository.findByUserIdAndProblemIdAndVerdict(userId, problemId, verdict, sortedPageable);
        } else if (verdict != null && userId != null) {
            submissions = submissionRepository.findByUserIdAndVerdict(userId, verdict, sortedPageable);
        } else if (verdict != null && problemId != null) {
            submissions = submissionRepository.findByProblemIdAndVerdict(problemId, verdict, sortedPageable);
        } else if (userId != null && problemId != null) {
            submissions = submissionRepository.findByUserIdAndProblemId(userId, problemId, sortedPageable);
        } else if (verdict != null) {
            submissions = submissionRepository.findByVerdict(verdict, sortedPageable);
        } else if (userId != null) {
            submissions = submissionRepository.findByUserId(userId, sortedPageable);
        } else if (problemId != null) {
            submissions = submissionRepository.findByProblemId(problemId, sortedPageable);
        } else {
            submissions = submissionRepository.findAll(sortedPageable);
        }

        return submissions.map(this::toDto);
    }

    @Override
    public UserStatsDto getUserStats(Long userId) {
        List<Submission> allSubmissions = submissionRepository.findByUserId(userId);

        Set<Long> solvedProblemIds = new HashSet<>();
        Set<Long> attemptedProblemIds = new HashSet<>();

        for (Submission submission : allSubmissions) {
            if (submission.getVerdict() == Verdict.ACCEPTED) {
                solvedProblemIds.add(submission.getProblemId());
            }
            attemptedProblemIds.add(submission.getProblemId());
        }

        attemptedProblemIds.removeAll(solvedProblemIds);

        int easySolved = 0;
        int mediumSolved = 0;
        int hardSolved = 0;

        if (!solvedProblemIds.isEmpty()) {
            List<Problem> solvedProblems = problemRepository.findAllById(solvedProblemIds);
            Map<Difficulty, Integer> difficultyCount = new HashMap<>();
            
            for (Problem problem : solvedProblems) {
                difficultyCount.merge(problem.getDifficulty(), 1, Integer::sum);
            }

            easySolved = difficultyCount.getOrDefault(Difficulty.EASY, 0);
            mediumSolved = difficultyCount.getOrDefault(Difficulty.MEDIUM, 0);
            hardSolved = difficultyCount.getOrDefault(Difficulty.HARD, 0);
        }

        return new UserStatsDto(
            solvedProblemIds.size(),
            attemptedProblemIds.size(),
            easySolved,
            mediumSolved,
            hardSolved
        );
    }

    private SubmissionDto toDto(Submission submission) {
        String problemTitle = problemRepository.findById(submission.getProblemId())
            .map(Problem::getTitle)
            .orElse("Unknown Problem");

        return new SubmissionDto(
            submission.getId(),
            submission.getUserId(),
            submission.getProblemId(),
            problemTitle,
            submission.getLanguageId(),
            submission.getCode(),
            submission.getVerdict(),
            submission.getRuntime(),
            submission.getMemory(),
            submission.getPassedTestcases(),
            submission.getTotalTestcases(),
            submission.getCreatedAt()
        );
    }
}
