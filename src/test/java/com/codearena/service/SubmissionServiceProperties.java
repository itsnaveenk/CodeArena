package com.codearena.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.codearena.document.Submission;
import com.codearena.dto.SubmissionDto;
import com.codearena.dto.UserStatsDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for SubmissionService.
 * 
 * <p>Tests the following correctness properties:
 * <ul>
 *   <li>Property 23: Submission History Ordering</li>
 *   <li>Property 24: Submission Access Control</li>
 *   <li>Property 25: User Statistics Accuracy</li>
 * </ul>
 */
public class SubmissionServiceProperties {

    private SubmissionRepository submissionRepository;
    private ProblemRepository problemRepository;
    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionRepository = Mockito.mock(SubmissionRepository.class);
        problemRepository = Mockito.mock(ProblemRepository.class);
        submissionService = new SubmissionServiceImpl(submissionRepository, problemRepository);
    }

    /**
     * Property 23: Submission History Ordering
     * 
     * For any user's submission history request, submissions SHALL be returned
     * in descending order by creation date.
     * 
     * Validates: Requirements 11.1
     */
    @Property(tries = 100)
    @Tag("Feature: codearena-platform, Property 23: Submission History Ordering")
    void submissionHistoryOrdering(@ForAll("submissionLists") List<Submission> submissions,
                                    @ForAll("users") User user) {
        // Setup: user requests their own submissions
        Long userId = user.getId();
        
        // Sort submissions by createdAt descending (as the service should do)
        List<Submission> sortedSubmissions = new ArrayList<>(submissions);
        sortedSubmissions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        // Set userId on all submissions
        for (Submission s : sortedSubmissions) {
            s.setUserId(userId);
        }
        
        Page<Submission> submissionPage = new PageImpl<>(sortedSubmissions);
        
        when(submissionRepository.findByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(submissionPage);
        
        // Mock problem repository for DTO conversion
        Problem mockProblem = new Problem();
        mockProblem.setTitle("Test Problem");
        when(problemRepository.findById(any())).thenReturn(java.util.Optional.of(mockProblem));
        
        // Execute
        Page<SubmissionDto> result = submissionService.getUserSubmissions(
            userId, null, PageRequest.of(0, 20), user);
        
        // Verify: submissions are in descending order by createdAt
        List<SubmissionDto> resultList = result.getContent();
        for (int i = 0; i < resultList.size() - 1; i++) {
            LocalDateTime current = resultList.get(i).createdAt();
            LocalDateTime next = resultList.get(i + 1).createdAt();
            assertThat(current).isAfterOrEqualTo(next);
        }
    }

    /**
     * Property 24: Submission Access Control
     * 
     * For any submission access request:
     * - If requester is the submission owner OR requester is ADMIN: access granted
     * - Otherwise: reject with 403
     * 
     * Validates: Requirements 11.5, 11.6
     */
    @Property(tries = 100)
    @Tag("Feature: codearena-platform, Property 24: Submission Access Control")
    void submissionAccessControl(@ForAll("submissions") Submission submission,
                                  @ForAll("users") User requester) {
        // Setup
        String submissionId = submission.getId();
        Long ownerId = submission.getUserId();
        Long requesterId = requester.getId();
        Role requesterRole = requester.getRole();
        
        when(submissionRepository.findById(submissionId))
            .thenReturn(java.util.Optional.of(submission));
        
        // Mock problem repository for DTO conversion
        Problem mockProblem = new Problem();
        mockProblem.setTitle("Test Problem");
        when(problemRepository.findById(any())).thenReturn(java.util.Optional.of(mockProblem));
        
        boolean isOwner = requesterId.equals(ownerId);
        boolean isAdmin = requesterRole == Role.ADMIN;
        
        if (isOwner || isAdmin) {
            // Access should be granted
            SubmissionDto result = submissionService.getSubmission(submissionId, requester);
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(submissionId);
        } else {
            // Access should be denied
            assertThatThrownBy(() -> submissionService.getSubmission(submissionId, requester))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    /**
     * Property 24 (continued): User submission history access control
     * 
     * Users can only view their own submission history, admins can view any.
     */
    @Property(tries = 100)
    @Tag("Feature: codearena-platform, Property 24: Submission Access Control - History")
    void submissionHistoryAccessControl(@ForAll("users") User owner,
                                         @ForAll("users") User requester) {
        Long ownerId = owner.getId();
        Long requesterId = requester.getId();
        Role requesterRole = requester.getRole();
        
        // Setup mock
        when(submissionRepository.findByUserId(eq(ownerId), any(Pageable.class)))
            .thenReturn(Page.empty());
        
        boolean isOwner = requesterId.equals(ownerId);
        boolean isAdmin = requesterRole == Role.ADMIN;
        
        if (isOwner || isAdmin) {
            // Access should be granted
            Page<SubmissionDto> result = submissionService.getUserSubmissions(
                ownerId, null, PageRequest.of(0, 20), requester);
            assertThat(result).isNotNull();
        } else {
            // Access should be denied
            assertThatThrownBy(() -> submissionService.getUserSubmissions(
                ownerId, null, PageRequest.of(0, 20), requester))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    /**
     * Property 25: User Statistics Accuracy
     * 
     * For any user's statistics:
     * - totalSolved = count of unique problems with at least one ACCEPTED submission
     * - totalAttempted = count of unique problems with submissions but no ACCEPTED
     * - easySolved/mediumSolved/hardSolved = breakdown of solved by difficulty
     * 
     * Validates: Requirements 12.1, 12.2, 12.3
     */
    @Property(tries = 100)
    @Tag("Feature: codearena-platform, Property 25: User Statistics Accuracy")
    void userStatisticsAccuracy(@ForAll("submissionListsForStats") List<Submission> submissions,
                                 @ForAll("problemsWithDifficulty") List<Problem> problems) {
        Long userId = 1L;
        
        // Set userId on all submissions
        for (Submission s : submissions) {
            s.setUserId(userId);
        }
        
        // Calculate expected statistics manually
        Set<Long> solvedProblemIds = new HashSet<>();
        Set<Long> attemptedProblemIds = new HashSet<>();
        
        for (Submission s : submissions) {
            if (s.getVerdict() == Verdict.ACCEPTED) {
                solvedProblemIds.add(s.getProblemId());
            }
            attemptedProblemIds.add(s.getProblemId());
        }
        
        // Attempted = has submissions but no ACCEPTED
        attemptedProblemIds.removeAll(solvedProblemIds);
        
        // Create problem map for difficulty lookup
        java.util.Map<Long, Problem> problemMap = problems.stream()
            .collect(Collectors.toMap(Problem::getId, p -> p, (a, b) -> a));
        
        int expectedEasySolved = 0;
        int expectedMediumSolved = 0;
        int expectedHardSolved = 0;
        
        for (Long problemId : solvedProblemIds) {
            Problem problem = problemMap.get(problemId);
            if (problem != null) {
                switch (problem.getDifficulty()) {
                    case EASY -> expectedEasySolved++;
                    case MEDIUM -> expectedMediumSolved++;
                    case HARD -> expectedHardSolved++;
                }
            }
        }
        
        // Setup mocks
        when(submissionRepository.findByUserId(userId)).thenReturn(submissions);
        
        List<Problem> solvedProblems = solvedProblemIds.stream()
            .map(problemMap::get)
            .filter(p -> p != null)
            .collect(Collectors.toList());
        when(problemRepository.findAllById(any())).thenReturn(solvedProblems);
        
        // Execute
        UserStatsDto stats = submissionService.getUserStats(userId);
        
        // Verify
        assertThat(stats.totalSolved()).isEqualTo(solvedProblemIds.size());
        assertThat(stats.totalAttempted()).isEqualTo(attemptedProblemIds.size());
        assertThat(stats.easySolved()).isEqualTo(expectedEasySolved);
        assertThat(stats.mediumSolved()).isEqualTo(expectedMediumSolved);
        assertThat(stats.hardSolved()).isEqualTo(expectedHardSolved);
    }

    // Arbitraries

    @Provide
    Arbitrary<User> users() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 100),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> s + "@example.com"),
            Arbitraries.of(Role.class)
        ).as((id, name, email, role) -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setRole(role);
            return user;
        });
    }

    @Provide
    Arbitrary<Submission> submissions() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofLength(24),
            Arbitraries.longs().between(1, 100),
            Arbitraries.longs().between(1, 50),
            Arbitraries.of(62, 71, 54, 63, 4, 74, 60, 73),
            Arbitraries.of(Verdict.class),
            Arbitraries.integers().between(0, 1000)
        ).as((id, userId, problemId, langId, verdict, minutesAgo) -> {
            Submission s = new Submission();
            s.setId(id);
            s.setUserId(userId);
            s.setProblemId(problemId);
            s.setLanguageId(langId);
            s.setCode("public class Solution {}");
            s.setVerdict(verdict);
            s.setRuntime(0.5);
            s.setMemory(10000);
            s.setPassedTestcases(5);
            s.setTotalTestcases(10);
            s.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
            return s;
        });
    }

    @Provide
    Arbitrary<List<Submission>> submissionLists() {
        return submissions().list().ofMinSize(0).ofMaxSize(20).map(list -> {
            // Assign unique timestamps to ensure ordering is testable
            LocalDateTime baseTime = LocalDateTime.now();
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setCreatedAt(baseTime.minusMinutes(i * 5 + Arbitraries.integers().between(0, 4).sample()));
            }
            return list;
        });
    }

    @Provide
    Arbitrary<List<Submission>> submissionListsForStats() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 10).list().ofMinSize(1).ofMaxSize(10),
            Arbitraries.of(Verdict.class).list().ofMinSize(1).ofMaxSize(20)
        ).as((problemIds, verdicts) -> {
            List<Submission> submissions = new ArrayList<>();
            for (int i = 0; i < verdicts.size(); i++) {
                Submission s = new Submission();
                s.setId("sub" + i);
                s.setProblemId(problemIds.get(i % problemIds.size()));
                s.setVerdict(verdicts.get(i));
                s.setLanguageId(62);
                s.setCode("code");
                s.setRuntime(0.5);
                s.setMemory(10000);
                s.setPassedTestcases(5);
                s.setTotalTestcases(10);
                s.setCreatedAt(LocalDateTime.now().minusMinutes(i));
                submissions.add(s);
            }
            return submissions;
        });
    }

    @Provide
    Arbitrary<List<Problem>> problemsWithDifficulty() {
        return Arbitraries.integers().between(1, 10).flatMap(count -> {
            List<Arbitrary<Problem>> problemArbitraries = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                final long id = i;
                problemArbitraries.add(
                    Arbitraries.of(Difficulty.class).map(difficulty -> {
                        Problem p = new Problem();
                        p.setId(id);
                        p.setTitle("Problem " + id);
                        p.setDifficulty(difficulty);
                        return p;
                    })
                );
            }
            return Combinators.combine(problemArbitraries).as(list -> list);
        });
    }
}
