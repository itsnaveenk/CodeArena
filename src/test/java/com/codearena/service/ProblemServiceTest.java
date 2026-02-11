package com.codearena.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import com.codearena.dto.CreateProblemRequest;
import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.ProblemFilter;
import com.codearena.dto.ProblemListDto;
import com.codearena.dto.ProblemSolveStatus;
import com.codearena.dto.UpdateProblemRequest;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.StarterCode;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.StarterCodeRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.TestcaseRepository;

/**
 * Unit tests for ProblemService.
 */
@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private TestcaseRepository testcaseRepository;

    @Mock
    private StarterCodeRepository starterCodeRepository;

    @Mock
    private AdminAuditService adminAuditService;

    private ProblemService problemService;

    @BeforeEach
    void setUp() {
        problemService = new ProblemServiceImpl(problemRepository, submissionRepository, 
                                                 testcaseRepository, starterCodeRepository, adminAuditService);
    }

    private User createUser(Long id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Problem createProblem(Long id, String title, Difficulty difficulty, ProblemStatus status) {
        User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
        Problem problem = new Problem(title, title.toLowerCase().replace(" ", "-"), 
                                      "Statement for " + title, difficulty, creator);
        problem.setId(id);
        problem.setStatus(status);
        problem.setTags(List.of("array", "dynamic-programming"));
        return problem;
    }

    @Nested
    @DisplayName("listPublishedProblems")
    class ListPublishedProblemsTests {

        @Test
        @DisplayName("should return published problems without filter")
        void shouldReturnPublishedProblemsWithoutFilter() {
            // Given
            Problem problem1 = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Problem problem2 = createProblem(2L, "Three Sum", Difficulty.MEDIUM, ProblemStatus.PUBLISHED);
            Page<Problem> problemPage = new PageImpl<>(List.of(problem1, problem2));
            Pageable pageable = PageRequest.of(0, 10);

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(
                ProblemFilter.empty(), pageable, null);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).title()).isEqualTo("Two Sum");
            assertThat(result.getContent().get(1).title()).isEqualTo("Three Sum");
            assertThat(result.getContent().get(0).solveStatus()).isNull();
        }

        @Test
        @DisplayName("should return problems with solve status for authenticated user")
        void shouldReturnProblemsWithSolveStatusForAuthenticatedUser() {
            // Given
            Long userId = 1L;
            Problem problem1 = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Problem problem2 = createProblem(2L, "Three Sum", Difficulty.MEDIUM, ProblemStatus.PUBLISHED);
            Problem problem3 = createProblem(3L, "Four Sum", Difficulty.HARD, ProblemStatus.PUBLISHED);
            Page<Problem> problemPage = new PageImpl<>(List.of(problem1, problem2, problem3));
            Pageable pageable = PageRequest.of(0, 10);

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);
            
            // User has ACCEPTED submission for problem 1
            when(submissionRepository.existsByUserIdAndProblemIdAndVerdict(userId, 1L, Verdict.ACCEPTED))
                .thenReturn(true);
            
            // User has submissions but no ACCEPTED for problem 2
            when(submissionRepository.existsByUserIdAndProblemIdAndVerdict(userId, 2L, Verdict.ACCEPTED))
                .thenReturn(false);
            when(submissionRepository.existsByUserIdAndProblemId(userId, 2L))
                .thenReturn(true);
            
            // User has no submissions for problem 3
            when(submissionRepository.existsByUserIdAndProblemIdAndVerdict(userId, 3L, Verdict.ACCEPTED))
                .thenReturn(false);
            when(submissionRepository.existsByUserIdAndProblemId(userId, 3L))
                .thenReturn(false);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(
                ProblemFilter.empty(), pageable, userId);

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).solveStatus()).isEqualTo(ProblemSolveStatus.SOLVED);
            assertThat(result.getContent().get(1).solveStatus()).isEqualTo(ProblemSolveStatus.ATTEMPTED);
            assertThat(result.getContent().get(2).solveStatus()).isEqualTo(ProblemSolveStatus.NOT_TRIED);
        }

        @Test
        @DisplayName("should return empty page when no problems match")
        void shouldReturnEmptyPageWhenNoProblemsMatch() {
            // Given
            Page<Problem> emptyPage = Page.empty();
            Pageable pageable = PageRequest.of(0, 10);

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(emptyPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(
                ProblemFilter.empty(), pageable, null);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should filter by difficulty")
        void shouldFilterByDifficulty() {
            // Given
            Problem problem = createProblem(1L, "Easy Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Page<Problem> problemPage = new PageImpl<>(List.of(problem));
            Pageable pageable = PageRequest.of(0, 10);
            ProblemFilter filter = new ProblemFilter(Difficulty.EASY, null, null);

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(filter, pageable, null);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).difficulty()).isEqualTo(Difficulty.EASY);
        }

        @Test
        @DisplayName("should filter by search keyword")
        void shouldFilterBySearchKeyword() {
            // Given
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Page<Problem> problemPage = new PageImpl<>(List.of(problem));
            Pageable pageable = PageRequest.of(0, 10);
            ProblemFilter filter = new ProblemFilter(null, null, "sum");

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(filter, pageable, null);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).containsIgnoringCase("sum");
        }

        @Test
        @DisplayName("should filter by tags")
        void shouldFilterByTags() {
            // Given
            Problem problem = createProblem(1L, "Array Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            problem.setTags(List.of("array", "hash-table"));
            Page<Problem> problemPage = new PageImpl<>(List.of(problem));
            Pageable pageable = PageRequest.of(0, 10);
            ProblemFilter filter = new ProblemFilter(null, List.of("array"), null);

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(filter, pageable, null);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).tags()).contains("array");
        }

        @Test
        @DisplayName("should combine multiple filters")
        void shouldCombineMultipleFilters() {
            // Given
            Problem problem = createProblem(1L, "Easy Array Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            problem.setTags(List.of("array"));
            Page<Problem> problemPage = new PageImpl<>(List.of(problem));
            Pageable pageable = PageRequest.of(0, 10);
            ProblemFilter filter = new ProblemFilter(Difficulty.EASY, List.of("array"), "sum");

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(filter, pageable, null);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).difficulty()).isEqualTo(Difficulty.EASY);
            assertThat(result.getContent().get(0).tags()).contains("array");
            assertThat(result.getContent().get(0).title()).containsIgnoringCase("sum");
        }

        @Test
        @DisplayName("should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Given
            Problem problem = createProblem(1L, "Problem 1", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Page<Problem> problemPage = new PageImpl<>(List.of(problem), PageRequest.of(1, 5), 10);
            Pageable pageable = PageRequest.of(1, 5);

            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.listPublishedProblems(
                ProblemFilter.empty(), pageable, null);

            // Then
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getProblemById")
    class GetProblemByIdTests {

        @Test
        @DisplayName("should return published problem for anonymous user")
        void shouldReturnPublishedProblemForAnonymousUser() {
            // Given
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of());
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of());

            // When
            ProblemDetailDto result = problemService.getProblemById(1L, null);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Two Sum");
            assertThat(result.status()).isEqualTo(ProblemStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent problem")
        void shouldThrowResourceNotFoundExceptionForNonExistentProblem() {
            // Given
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.getProblemById(999L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-published problem when user is anonymous")
        void shouldThrowResourceNotFoundExceptionForNonPublishedProblemWhenAnonymous() {
            // Given
            Problem problem = createProblem(1L, "Draft Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.getProblemById(1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should return non-published problem for creator")
        void shouldReturnNonPublishedProblemForCreator() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Draft Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of());
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of());

            // When
            ProblemDetailDto result = problemService.getProblemById(1L, creator);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(ProblemStatus.DRAFT);
        }

        @Test
        @DisplayName("should return any problem for admin")
        void shouldReturnAnyProblemForAdmin() {
            // Given
            User admin = createUser(2L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Draft Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of());
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of());

            // When
            ProblemDetailDto result = problemService.getProblemById(1L, admin);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(ProblemStatus.DRAFT);
        }

        @Test
        @DisplayName("should include visible testcases as examples")
        void shouldIncludeVisibleTestcasesAsExamples() {
            // Given
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Testcase visibleTestcase = new Testcase(problem, "1 2 3", "6", false);
            visibleTestcase.setId(1L);
            Testcase hiddenTestcase = new Testcase(problem, "4 5 6", "15", true);
            hiddenTestcase.setId(2L);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of(visibleTestcase, hiddenTestcase));
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of());

            // When
            ProblemDetailDto result = problemService.getProblemById(1L, null);

            // Then
            assertThat(result.examples()).hasSize(1);
            assertThat(result.examples().get(0).input()).isEqualTo("1 2 3");
            assertThat(result.examples().get(0).isHidden()).isFalse();
        }

        @Test
        @DisplayName("should include all testcases for admin")
        void shouldIncludeAllTestcasesForAdmin() {
            // Given
            User admin = createUser(2L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            Testcase visibleTestcase = new Testcase(problem, "1 2 3", "6", false);
            visibleTestcase.setId(1L);
            Testcase hiddenTestcase = new Testcase(problem, "4 5 6", "15", true);
            hiddenTestcase.setId(2L);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of(visibleTestcase, hiddenTestcase));
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of());

            // When
            ProblemDetailDto result = problemService.getProblemById(1L, admin);

            // Then
            assertThat(result.examples()).hasSize(2);
        }

        @Test
        @DisplayName("should include starter code")
        void shouldIncludeStarterCode() {
            // Given
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            StarterCode javaCode = new StarterCode(problem, 62, "class Solution {}");
            javaCode.setId(1L);
            StarterCode pythonCode = new StarterCode(problem, 71, "def solution():");
            pythonCode.setId(2L);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of());
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of(javaCode, pythonCode));

            // When
            ProblemDetailDto result = problemService.getProblemById(1L, null);

            // Then
            assertThat(result.starterCode()).hasSize(2);
            assertThat(result.starterCode().get("62")).isEqualTo("class Solution {}");
            assertThat(result.starterCode().get("71")).isEqualTo("def solution():");
        }
    }

    @Nested
    @DisplayName("getProblemBySlug")
    class GetProblemBySlugTests {

        @Test
        @DisplayName("should return published problem by slug")
        void shouldReturnPublishedProblemBySlug() {
            // Given
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of());
            when(starterCodeRepository.findByProblemId(1L)).thenReturn(List.of());

            // When
            ProblemDetailDto result = problemService.getProblemBySlug("two-sum", null);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.slug()).isEqualTo("two-sum");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent slug")
        void shouldThrowResourceNotFoundExceptionForNonExistentSlug() {
            // Given
            when(problemRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.getProblemBySlug("non-existent", null))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should apply same access control as getProblemById")
        void shouldApplySameAccessControlAsGetProblemById() {
            // Given
            Problem problem = createProblem(1L, "Draft Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            when(problemRepository.findBySlug("draft-problem")).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.getProblemBySlug("draft-problem", null))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createProblem")
    class CreateProblemTests {

        @Test
        @DisplayName("should create problem with required fields")
        void shouldCreateProblemWithRequiredFields() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            CreateProblemRequest request = CreateProblemRequest.of(
                "Two Sum", "Find two numbers that add up to target", Difficulty.EASY);
            
            when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
                Problem p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });

            // When
            Problem result = problemService.createProblem(request, creator);

            // Then
            assertThat(result.getTitle()).isEqualTo("Two Sum");
            assertThat(result.getSlug()).isEqualTo("two-sum");
            assertThat(result.getStatement()).isEqualTo("Find two numbers that add up to target");
            assertThat(result.getDifficulty()).isEqualTo(Difficulty.EASY);
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.DRAFT);
            assertThat(result.getCreatedBy()).isEqualTo(creator);
        }

        @Test
        @DisplayName("should create problem with all optional fields")
        void shouldCreateProblemWithAllOptionalFields() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            CreateProblemRequest request = new CreateProblemRequest(
                "Two Sum",
                "Find two numbers that add up to target",
                "1 <= nums.length <= 10^4",
                Difficulty.EASY,
                List.of("array", "hash-table"),
                Map.of("62", "class Solution {}", "71", "def solution():")
            );
            
            when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
                Problem p = invocation.getArgument(0);
                if (p.getId() == null) {
                    p.setId(1L);
                }
                return p;
            });

            // When
            Problem result = problemService.createProblem(request, creator);

            // Then
            assertThat(result.getConstraints()).isEqualTo("1 <= nums.length <= 10^4");
            assertThat(result.getTags()).containsExactlyInAnyOrder("array", "hash-table");
            assertThat(result.getStarterCodes()).hasSize(2);
        }

        @Test
        @DisplayName("should generate unique slug when duplicate exists")
        void shouldGenerateUniqueSlugWhenDuplicateExists() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            CreateProblemRequest request = CreateProblemRequest.of(
                "Two Sum", "Statement", Difficulty.EASY);
            
            Problem existingProblem = createProblem(99L, "Two Sum", Difficulty.EASY, ProblemStatus.PUBLISHED);
            
            when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(existingProblem));
            when(problemRepository.findBySlug("two-sum-1")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
                Problem p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });

            // When
            Problem result = problemService.createProblem(request, creator);

            // Then
            assertThat(result.getSlug()).isEqualTo("two-sum-1");
        }

        @Test
        @DisplayName("should set status to DRAFT")
        void shouldSetStatusToDraft() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            CreateProblemRequest request = CreateProblemRequest.of(
                "New Problem", "Statement", Difficulty.MEDIUM);
            
            when(problemRepository.findBySlug("new-problem")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
                Problem p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });

            // When
            Problem result = problemService.createProblem(request, creator);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.DRAFT);
        }

        @Test
        @DisplayName("should set createdBy to creator")
        void shouldSetCreatedByToCreator() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            CreateProblemRequest request = CreateProblemRequest.of(
                "New Problem", "Statement", Difficulty.HARD);
            
            when(problemRepository.findBySlug("new-problem")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
                Problem p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });

            // When
            Problem result = problemService.createProblem(request, creator);

            // Then
            assertThat(result.getCreatedBy()).isEqualTo(creator);
            assertThat(result.getCreatedBy().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("updateProblem")
    class UpdateProblemTests {

        @Test
        @DisplayName("should update problem title and regenerate slug")
        void shouldUpdateProblemTitleAndRegenerateSlug() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Old Title", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "New Title", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.findBySlug("new-title")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, creator);

            // Then
            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getSlug()).isEqualTo("new-title");
        }

        @Test
        @DisplayName("should update problem statement")
        void shouldUpdateProblemStatement() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                null, "Updated statement", null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, creator);

            // Then
            assertThat(result.getStatement()).isEqualTo("Updated statement");
        }

        @Test
        @DisplayName("should update problem difficulty")
        void shouldUpdateProblemDifficulty() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                null, null, null, Difficulty.HARD, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, creator);

            // Then
            assertThat(result.getDifficulty()).isEqualTo(Difficulty.HARD);
        }

        @Test
        @DisplayName("should update problem tags")
        void shouldUpdateProblemTags() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            problem.setTags(List.of("old-tag"));
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                null, null, null, null, List.of("new-tag-1", "new-tag-2"), null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, creator);

            // Then
            assertThat(result.getTags()).containsExactlyInAnyOrder("new-tag-1", "new-tag-2");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-creator tries to edit")
        void shouldThrowAccessDeniedExceptionWhenNonCreatorTriesToEdit() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            User otherUser = createUser(2L, "Other", "other@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "New Title", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.updateProblem(1L, request, otherUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only edit your own problems");
        }

        @Test
        @DisplayName("should allow admin to edit any problem")
        void shouldAllowAdminToEditAnyProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            User admin = createUser(2L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "Admin Updated", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.findBySlug("admin-updated")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, admin);

            // Then
            assertThat(result.getTitle()).isEqualTo("Admin Updated");
        }

        @Test
        @DisplayName("should throw IllegalStateException when editing PENDING_REVIEW problem as non-admin")
        void shouldThrowIllegalStateExceptionWhenEditingPendingReviewProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PENDING_REVIEW);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "New Title", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.updateProblem(1L, request, creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT or REJECTED");
        }

        @Test
        @DisplayName("should throw IllegalStateException when editing PUBLISHED problem as non-admin")
        void shouldThrowIllegalStateExceptionWhenEditingPublishedProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "New Title", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.updateProblem(1L, request, creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT or REJECTED");
        }

        @Test
        @DisplayName("should allow editing REJECTED problem")
        void shouldAllowEditingRejectedProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.REJECTED);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "Fixed Problem", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.findBySlug("fixed-problem")).thenReturn(Optional.empty());
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, creator);

            // Then
            assertThat(result.getTitle()).isEqualTo("Fixed Problem");
        }

        @Test
        @DisplayName("should allow admin to edit PUBLISHED problem")
        void shouldAllowAdminToEditPublishedProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            User admin = createUser(2L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            problem.setCreatedBy(creator);
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                null, "Updated by admin", null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, admin);

            // Then
            assertThat(result.getStatement()).isEqualTo("Updated by admin");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent problem")
        void shouldThrowResourceNotFoundExceptionForNonExistentProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            UpdateProblemRequest request = new UpdateProblemRequest(
                "New Title", null, null, null, null, null);
            
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.updateProblem(999L, request, creator))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should keep same slug when title unchanged but slug already exists for same problem")
        void shouldKeepSameSlugWhenTitleUnchanged() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Two Sum", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            problem.setSlug("two-sum");
            
            UpdateProblemRequest request = new UpdateProblemRequest(
                "Two Sum", null, null, null, null, null);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.updateProblem(1L, request, creator);

            // Then
            assertThat(result.getSlug()).isEqualTo("two-sum");
        }
    }

    @Nested
    @DisplayName("requestReview")
    class RequestReviewTests {

        @Test
        @DisplayName("should change status to PENDING_REVIEW when valid")
        void shouldChangeStatusToPendingReviewWhenValid() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn(1L);
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, true)).thenReturn(1L);
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.requestReview(1L, creator);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("should allow admin to request review for any problem")
        void shouldAllowAdminToRequestReviewForAnyProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            User admin = createUser(2L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn(1L);
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, true)).thenReturn(1L);
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.requestReview(1L, admin);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-creator requests review")
        void shouldThrowAccessDeniedExceptionWhenNonCreatorRequestsReview() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            User otherUser = createUser(2L, "Other", "other@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.requestReview(1L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException when problem is not in DRAFT status")
        void shouldThrowIllegalStateExceptionWhenNotDraft() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.requestReview(1L, creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("should throw IllegalStateException when no visible testcases")
        void shouldThrowIllegalStateExceptionWhenNoVisibleTestcases() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn(0L);

            // When/Then
            assertThatThrownBy(() -> problemService.requestReview(1L, creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("visible testcase");
        }

        @Test
        @DisplayName("should throw IllegalStateException when no hidden testcases")
        void shouldThrowIllegalStateExceptionWhenNoHiddenTestcases() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            problem.setCreatedBy(creator);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn(1L);
            when(testcaseRepository.countByProblemIdAndIsHidden(1L, true)).thenReturn(0L);

            // When/Then
            assertThatThrownBy(() -> problemService.requestReview(1L, creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hidden testcase");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent problem")
        void shouldThrowResourceNotFoundExceptionForNonExistentProblem() {
            // Given
            User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.requestReview(999L, creator))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("publishProblem")
    class PublishProblemTests {

        @Test
        @DisplayName("should change status to PUBLISHED when admin publishes")
        void shouldChangeStatusToPublishedWhenAdminPublishes() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PENDING_REVIEW);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.publishProblem(1L, admin);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-admin tries to publish")
        void shouldThrowAccessDeniedExceptionWhenNonAdminTriesToPublish() {
            // Given
            User user = createUser(1L, "User", "user@example.com", Role.PROBLEM_SETTER);

            // When/Then
            assertThatThrownBy(() -> problemService.publishProblem(1L, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admin");
        }

        @Test
        @DisplayName("should throw IllegalStateException when problem is not PENDING_REVIEW")
        void shouldThrowIllegalStateExceptionWhenNotPendingReview() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.DRAFT);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.publishProblem(1L, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_REVIEW");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent problem")
        void shouldThrowResourceNotFoundExceptionForNonExistentProblem() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.publishProblem(999L, admin))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectProblem")
    class RejectProblemTests {

        @Test
        @DisplayName("should change status to REJECTED when admin rejects")
        void shouldChangeStatusToRejectedWhenAdminRejects() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PENDING_REVIEW);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.rejectProblem(1L, "test", admin);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-admin tries to reject")
        void shouldThrowAccessDeniedExceptionWhenNonAdminTriesToReject() {
            // Given
            User user = createUser(1L, "User", "user@example.com", Role.PROBLEM_SETTER);

            // When/Then
            assertThatThrownBy(() -> problemService.rejectProblem(1L, "test,", user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admin");
        }

        @Test
        @DisplayName("should throw IllegalStateException when problem is not PENDING_REVIEW")
        void shouldThrowIllegalStateExceptionWhenNotPendingReview() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            // When/Then
            assertThatThrownBy(() -> problemService.rejectProblem(1L, "test", admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_REVIEW");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent problem")
        void shouldThrowResourceNotFoundExceptionForNonExistentProblem() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.rejectProblem(999L, "test", admin))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("archiveProblem")
    class ArchiveProblemTests {

        @Test
        @DisplayName("should change status to ARCHIVED when admin archives")
        void shouldChangeStatusToArchivedWhenAdminArchives() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem = createProblem(1L, "Problem", Difficulty.EASY, ProblemStatus.PUBLISHED);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.archiveProblem(1L, admin);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should allow archiving from any status")
        void shouldAllowArchivingFromAnyStatus() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem draftProblem = createProblem(1L, "Draft", Difficulty.EASY, ProblemStatus.DRAFT);
            
            when(problemRepository.findById(1L)).thenReturn(Optional.of(draftProblem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Problem result = problemService.archiveProblem(1L, admin);

            // Then
            assertThat(result.getStatus()).isEqualTo(ProblemStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-admin tries to archive")
        void shouldThrowAccessDeniedExceptionWhenNonAdminTriesToArchive() {
            // Given
            User user = createUser(1L, "User", "user@example.com", Role.PROBLEM_SETTER);

            // When/Then
            assertThatThrownBy(() -> problemService.archiveProblem(1L, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admin");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent problem")
        void shouldThrowResourceNotFoundExceptionForNonExistentProblem() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> problemService.archiveProblem(999L, admin))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPendingProblems")
    class GetPendingProblemsTests {

        @Test
        @DisplayName("should return pending problems for admin")
        void shouldReturnPendingProblemsForAdmin() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Problem problem1 = createProblem(1L, "Problem 1", Difficulty.EASY, ProblemStatus.PENDING_REVIEW);
            Problem problem2 = createProblem(2L, "Problem 2", Difficulty.MEDIUM, ProblemStatus.PENDING_REVIEW);
            Page<Problem> problemPage = new PageImpl<>(List.of(problem1, problem2));
            Pageable pageable = PageRequest.of(0, 10);
            
            when(problemRepository.findByStatus(ProblemStatus.PENDING_REVIEW, pageable))
                .thenReturn(problemPage);

            // When
            Page<ProblemListDto> result = problemService.getPendingProblems(admin, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).title()).isEqualTo("Problem 1");
            assertThat(result.getContent().get(1).title()).isEqualTo("Problem 2");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-admin requests pending problems")
        void shouldThrowAccessDeniedExceptionWhenNonAdminRequestsPendingProblems() {
            // Given
            User user = createUser(1L, "User", "user@example.com", Role.PROBLEM_SETTER);
            Pageable pageable = PageRequest.of(0, 10);

            // When/Then
            assertThatThrownBy(() -> problemService.getPendingProblems(user, pageable))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admin");
        }

        @Test
        @DisplayName("should return empty page when no pending problems")
        void shouldReturnEmptyPageWhenNoPendingProblems() {
            // Given
            User admin = createUser(1L, "Admin", "admin@example.com", Role.ADMIN);
            Page<Problem> emptyPage = Page.empty();
            Pageable pageable = PageRequest.of(0, 10);
            
            when(problemRepository.findByStatus(ProblemStatus.PENDING_REVIEW, pageable))
                .thenReturn(emptyPage);

            // When
            Page<ProblemListDto> result = problemService.getPendingProblems(admin, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }
}
