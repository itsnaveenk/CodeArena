package com.codearena.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.codearena.document.Submission;
import com.codearena.dto.PlatformStatsDto;
import com.codearena.dto.ProblemManagementDto;
import com.codearena.dto.UserListDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final AdminAuditService adminAuditService;

    public AdminServiceImpl(UserRepository userRepository,
                            ProblemRepository problemRepository,
                            SubmissionRepository submissionRepository,
                            AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.adminAuditService = adminAuditService;
    }

    @Override
    public PlatformStatsDto getPlatformStats() {
        Map<Role, Long> usersByRole = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            usersByRole.put(role, userRepository.countByRole(role));
        }
        long totalUsers = usersByRole.values().stream().mapToLong(Long::longValue).sum();

        Map<ProblemStatus, Long> problemsByStatus = new EnumMap<>(ProblemStatus.class);
        for (ProblemStatus status : ProblemStatus.values()) {
            problemsByStatus.put(status, problemRepository.countByStatus(status));
        }
        long totalProblems = problemsByStatus.values().stream().mapToLong(Long::longValue).sum();

        Map<Difficulty, Long> problemsByDifficulty = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            problemsByDifficulty.put(difficulty, problemRepository.countByDifficulty(difficulty));
        }

        long totalSubmissions = submissionRepository.count();
        LocalDateTime todayStart = LocalDateTime.now().minusHours(24);
        long submissionsToday = submissionRepository.countByCreatedAtAfter(todayStart);

        double acceptanceRate = 0.0;
        if (totalSubmissions > 0) {
            long acceptedCount = submissionRepository.countByVerdict(Verdict.ACCEPTED);
            acceptanceRate = (double) acceptedCount / totalSubmissions * 100;
        }

        return new PlatformStatsDto(
            totalUsers,
            usersByRole,
            totalProblems,
            problemsByStatus,
            problemsByDifficulty,
            totalSubmissions,
            submissionsToday,
            Math.round(acceptanceRate * 100.0) / 100.0
        );
    }

    @Override
    public Page<UserListDto> listUsers(Role role, String search, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<User> users;

        boolean hasSearch = StringUtils.hasText(search);
        boolean hasRole = role != null;

        if (hasRole && hasSearch) {
            users = userRepository.findByRoleAndNameOrEmailContaining(role, search, search, sortedPageable);
        } else if (hasRole) {
            users = userRepository.findByRole(role, sortedPageable);
        } else if (hasSearch) {
            users = userRepository.findByNameOrEmailContaining(search, search, sortedPageable);
        } else {
            users = userRepository.findAll(sortedPageable);
        }

        return users.map(user -> {
            int solvedCount = calculateSolvedProblems(user.getId());
            return UserListDto.fromEntity(user, solvedCount);
        });
    }

    @Override
    public Page<ProblemManagementDto> listAllProblems(
            ProblemStatus status,
            Difficulty difficulty,
            String search,
            Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Problem> problems;

        boolean hasSearch = StringUtils.hasText(search);
        boolean hasStatus = status != null;
        boolean hasDifficulty = difficulty != null;

        if (hasStatus && hasDifficulty && hasSearch) {
            problems = problemRepository.findByStatusAndDifficultyAndTitleOrAuthorContaining(
                status, difficulty, search, sortedPageable);
        } else if (hasStatus && hasDifficulty) {
            problems = problemRepository.findByStatusAndDifficulty(status, difficulty, sortedPageable);
        } else if (hasStatus && hasSearch) {
            problems = problemRepository.findByStatusAndTitleOrAuthorContaining(status, search, sortedPageable);
        } else if (hasDifficulty && hasSearch) {
            problems = problemRepository.findByDifficultyAndTitleOrAuthorContaining(difficulty, search, sortedPageable);
        } else if (hasStatus) {
            problems = problemRepository.findByStatus(status, sortedPageable);
        } else if (hasDifficulty) {
            problems = problemRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty),
                sortedPageable
            );
        } else if (hasSearch) {
            problems = problemRepository.findByTitleOrAuthorContaining(search, sortedPageable);
        } else {
            problems = problemRepository.findAll(sortedPageable);
        }

        return problems.map(problem -> {
            long submissionCount = submissionRepository.countByProblemId(problem.getId());
            return ProblemManagementDto.fromEntity(problem, submissionCount);
        });
    }

    @Override
    @Transactional
    public int bulkPublish(List<Long> problemIds, User admin) {
        List<Problem> problems = problemRepository.findByIdIn(problemIds);
        int publishedCount = 0;

        for (Problem problem : problems) {
            if (problem.getStatus() == ProblemStatus.PENDING_REVIEW) {
                problem.setStatus(ProblemStatus.PUBLISHED);
                problemRepository.save(problem);
                publishedCount++;
                logger.info("Admin {} bulk-published problem {}: {}",
                    admin.getEmail(), problem.getId(), problem.getTitle());
            }
        }

        adminAuditService.record(
            admin,
            com.codearena.entity.AdminAuditAction.PROBLEM_BULK_PUBLISHED,
            com.codearena.entity.AdminAuditTargetType.PROBLEM,
            null,
            java.util.Map.of(
                "count", publishedCount,
                "ids", problemIds
            )
        );

        return publishedCount;
    }

    @Override
    @Transactional
    public int bulkReject(List<Long> problemIds, User admin) {
        List<Problem> problems = problemRepository.findByIdIn(problemIds);
        int rejectedCount = 0;

        for (Problem problem : problems) {
            if (problem.getStatus() == ProblemStatus.PENDING_REVIEW) {
                problem.setStatus(ProblemStatus.REJECTED);
                problemRepository.save(problem);
                rejectedCount++;
                logger.info("Admin {} bulk-rejected problem {}: {}",
                    admin.getEmail(), problem.getId(), problem.getTitle());
            }
        }

        adminAuditService.record(
            admin,
            com.codearena.entity.AdminAuditAction.PROBLEM_BULK_REJECTED,
            com.codearena.entity.AdminAuditTargetType.PROBLEM,
            null,
            java.util.Map.of(
                "count", rejectedCount,
                "ids", problemIds
            )
        );

        return rejectedCount;
    }

    @Override
    @Transactional
    public int bulkArchive(List<Long> problemIds, User admin) {
        List<Problem> problems = problemRepository.findByIdIn(problemIds);
        int archivedCount = 0;

        for (Problem problem : problems) {
            if (problem.getStatus() == ProblemStatus.PUBLISHED) {
                problem.setStatus(ProblemStatus.ARCHIVED);
                problemRepository.save(problem);
                archivedCount++;
                logger.info("Admin {} bulk-archived problem {}: {}",
                    admin.getEmail(), problem.getId(), problem.getTitle());
            }
        }

        adminAuditService.record(
            admin,
            com.codearena.entity.AdminAuditAction.PROBLEM_BULK_ARCHIVED,
            com.codearena.entity.AdminAuditTargetType.PROBLEM,
            null,
            java.util.Map.of(
                "count", archivedCount,
                "ids", problemIds
            )
        );

        return archivedCount;
    }

    private int calculateSolvedProblems(Long userId) {
        List<Submission> submissions = submissionRepository.findByUserId(userId);
        Set<Long> solvedProblemIds = new HashSet<>();

        for (Submission submission : submissions) {
            if (submission.getVerdict() == Verdict.ACCEPTED) {
                solvedProblemIds.add(submission.getProblemId());
            }
        }

        return solvedProblemIds.size();
    }
}
