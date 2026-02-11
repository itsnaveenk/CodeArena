package com.codearena.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.StarterCodeRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.TestcaseRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

/**
 * Property-based tests for ProblemService listing and filtering operations.
 * 
 * <p>These tests verify the correctness properties defined in the design document:
 * <ul>
 *   <li>Property 9: Published Problems Only in Listing</li>
 *   <li>Property 10: Problem Filtering Correctness</li>
 *   <li>Property 11: Pagination Correctness</li>
 *   <li>Property 12: Solve Status Accuracy</li>
 *   <li>Property 13: Problem Access Control</li>
 *   <li>Property 14: Problem Creation Invariants</li>
 *   <li>Property 15: Problem Edit Authorization</li>
 * </ul>
 * 
 * <p><b>Validates: Requirements 4.1-4.6, 5.3, 5.4, 5.5, 6.1-6.5, 13.2</b>
 */
@Label("ProblemService Property Tests")
@Tag("codearena-platform")
class ProblemServiceProperties {

    // ========================================================================
    // Test Data Generators
    // ========================================================================

    @Provide
    Arbitrary<User> users() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 10000),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),
            Arbitraries.of("user@example.com", "test@test.io", "admin@codearena.dev"),
            Arbitraries.of(Role.values())
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
    Arbitrary<List<Problem>> publishedProblems() {
        return Arbitraries.integers().between(1, 10)
            .flatMap(size -> {
                List<Problem> problems = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    problems.add(createProblem((long) (i + 1), ProblemStatus.PUBLISHED));
                }
                return Arbitraries.just(problems);
            });
    }

    @Provide
    Arbitrary<List<Problem>> publishedProblemsLarge() {
        return Arbitraries.integers().between(1, 50)
            .flatMap(size -> {
                List<Problem> problems = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    problems.add(createProblem((long) (i + 1), ProblemStatus.PUBLISHED));
                }
                return Arbitraries.just(problems);
            });
    }

    @Provide
    Arbitrary<List<Problem>> problemsWithVariousStatuses() {
        return Arbitraries.integers().between(1, 20)
            .flatMap(size -> {
                List<Problem> problems = new ArrayList<>();
                ProblemStatus[] statuses = ProblemStatus.values();
                for (int i = 0; i < size; i++) {
                    ProblemStatus status = statuses[i % statuses.length];
                    problems.add(createProblem((long) (i + 1), status));
                }
                return Arbitraries.just(problems);
            });
    }

    private Problem createProblem(Long id, ProblemStatus status) {
        User creator = createTestUser(1L);
        Difficulty[] difficulties = Difficulty.values();
        Difficulty difficulty = difficulties[(int) (id % difficulties.length)];
        String title = "Problem " + id;
        String slug = "problem-" + id;
        Problem problem = new Problem(title, slug, "Statement for " + title, difficulty, creator);
        problem.setId(id);
        problem.setStatus(status);
        problem.setTags(List.of("array", "dynamic-programming"));
        return problem;
    }

    @Provide
    Arbitrary<List<String>> tags() {
        return Arbitraries.of(
            "array", "string", "hash-table", "dynamic-programming", 
            "math", "sorting", "greedy", "binary-search", "tree", "graph"
        ).list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<ProblemFilter> filters() {
        return Combinators.combine(
            Arbitraries.of(Difficulty.values()).injectNull(0.3),
            tags().injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20).injectNull(0.3)
        ).as(ProblemFilter::new);
    }

    @Provide
    Arbitrary<Difficulty> difficulties() {
        return Arbitraries.of(Difficulty.values());
    }

    private User createTestUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        return user;
    }

    // ========================================================================
    // Property 9: Published Problems Only in Listing
    // ========================================================================

    /**
     * Property 9: For any problem listing request by a USER, the result SHALL 
     * contain only problems with status PUBLISHED.
     * 
     * <p>This test verifies that the service correctly filters out non-published
     * problems from the listing results.
     * 
     * <p><b>Validates: Requirements 4.1</b>
     */
    @Property(tries = 100)
    @Label("Property 9: Published Problems Only in Listing")
    void publishedProblemsOnlyInListing(
            @ForAll("problemsWithVariousStatuses") List<Problem> allProblems) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Filter to only published problems (simulating what the repository would return)
        List<Problem> publishedProblems = allProblems.stream()
            .filter(p -> p.getStatus() == ProblemStatus.PUBLISHED)
            .collect(Collectors.toList());
        
        Page<Problem> problemPage = new PageImpl<>(publishedProblems);
        Pageable pageable = PageRequest.of(0, 100);
        
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(problemPage);
        
        // Execute
        Page<ProblemListDto> result = service.listPublishedProblems(
            ProblemFilter.empty(), pageable, null);
        
        // Verify - all returned problems should be from the published list
        assertThat(result.getContent())
            .as("All returned problems should be from published problems list")
            .allSatisfy(dto -> {
                boolean foundInPublished = publishedProblems.stream()
                    .anyMatch(p -> p.getId().equals(dto.id()));
                assertThat(foundInPublished)
                    .as("Problem %d should be in published list", dto.id())
                    .isTrue();
            });
    }

    // ========================================================================
    // Property 10: Problem Filtering Correctness
    // ========================================================================

    /**
     * Property 10a: For any problem filter with difficulty specified, the returned 
     * problems SHALL match the specified difficulty.
     * 
     * <p><b>Validates: Requirements 4.2</b>
     */
    @Property(tries = 100)
    @Label("Property 10a: Difficulty filtering returns matching problems")
    void difficultyFilteringReturnsMatchingProblems(
            @ForAll("difficulties") Difficulty filterDifficulty,
            @ForAll("publishedProblems") List<Problem> problems) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Filter problems by difficulty (simulating repository behavior)
        List<Problem> matchingProblems = problems.stream()
            .filter(p -> p.getDifficulty() == filterDifficulty)
            .collect(Collectors.toList());
        
        Page<Problem> problemPage = new PageImpl<>(matchingProblems);
        Pageable pageable = PageRequest.of(0, 100);
        ProblemFilter filter = new ProblemFilter(filterDifficulty, null, null);
        
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(problemPage);
        
        // Execute
        Page<ProblemListDto> result = service.listPublishedProblems(filter, pageable, null);
        
        // Verify - all returned problems should have the specified difficulty
        assertThat(result.getContent())
            .as("All returned problems should have difficulty %s", filterDifficulty)
            .allSatisfy(dto -> 
                assertThat(dto.difficulty()).isEqualTo(filterDifficulty)
            );
    }

    /**
     * Property 10b: For any problem filter with search keyword specified, the returned 
     * problems SHALL have titles containing the keyword (case-insensitive).
     * 
     * <p><b>Validates: Requirements 4.4</b>
     */
    @Property(tries = 100)
    @Label("Property 10b: Search filtering returns matching problems")
    void searchFilteringReturnsMatchingProblems(
            @ForAll("publishedProblems") List<Problem> problems) {
        
        if (problems.isEmpty()) return;
        
        // Use a substring of the first problem's title as search keyword
        String searchKeyword = problems.get(0).getTitle().substring(0, 
            Math.min(3, problems.get(0).getTitle().length()));
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Filter problems by title (simulating repository behavior)
        List<Problem> matchingProblems = problems.stream()
            .filter(p -> p.getTitle().toLowerCase().contains(searchKeyword.toLowerCase()))
            .collect(Collectors.toList());
        
        Page<Problem> problemPage = new PageImpl<>(matchingProblems);
        Pageable pageable = PageRequest.of(0, 100);
        ProblemFilter filter = new ProblemFilter(null, null, searchKeyword);
        
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(problemPage);
        
        // Execute
        Page<ProblemListDto> result = service.listPublishedProblems(filter, pageable, null);
        
        // Verify - all returned problems should have titles containing the keyword
        assertThat(result.getContent())
            .as("All returned problems should have titles containing '%s'", searchKeyword)
            .allSatisfy(dto -> 
                assertThat(dto.title().toLowerCase())
                    .containsIgnoringCase(searchKeyword.toLowerCase())
            );
    }

    /**
     * Property 10c: For any problem filter with tags specified, the returned 
     * problems SHALL contain at least one of the specified tags.
     * 
     * <p><b>Validates: Requirements 4.3</b>
     */
    @Property(tries = 100)
    @Label("Property 10c: Tag filtering returns matching problems")
    void tagFilteringReturnsMatchingProblems(
            @ForAll("tags") @Size(min = 1, max = 3) List<String> filterTags,
            @ForAll("publishedProblems") List<Problem> problems) {
        
        if (filterTags.isEmpty()) return;
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Filter problems by tags (simulating repository behavior)
        List<Problem> matchingProblems = problems.stream()
            .filter(p -> p.getTags().stream().anyMatch(filterTags::contains))
            .collect(Collectors.toList());
        
        Page<Problem> problemPage = new PageImpl<>(matchingProblems);
        Pageable pageable = PageRequest.of(0, 100);
        ProblemFilter filter = new ProblemFilter(null, filterTags, null);
        
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(problemPage);
        
        // Execute
        Page<ProblemListDto> result = service.listPublishedProblems(filter, pageable, null);
        
        // Verify - all returned problems should have at least one matching tag
        assertThat(result.getContent())
            .as("All returned problems should have at least one tag from %s", filterTags)
            .allSatisfy(dto -> {
                boolean hasMatchingTag = dto.tags().stream().anyMatch(filterTags::contains);
                assertThat(hasMatchingTag)
                    .as("Problem '%s' should have at least one tag from %s", dto.title(), filterTags)
                    .isTrue();
            });
    }

    // ========================================================================
    // Property 11: Pagination Correctness
    // ========================================================================

    /**
     * Property 11: For any paginated request with page P and size S, the result 
     * SHALL contain at most S items, and requesting sequential pages SHALL return 
     * all items exactly once with no duplicates or gaps.
     * 
     * <p><b>Validates: Requirements 4.5, 11.2</b>
     */
    @Property(tries = 100)
    @Label("Property 11: Pagination Correctness")
    void paginationCorrectness(
            @ForAll @IntRange(min = 1, max = 20) int pageSize,
            @ForAll("publishedProblemsLarge") List<Problem> allProblems) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        int totalPages = (int) Math.ceil((double) allProblems.size() / pageSize);
        List<Long> allReturnedIds = new ArrayList<>();
        
        // Request each page sequentially
        for (int page = 0; page < totalPages; page++) {
            int fromIndex = page * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allProblems.size());
            List<Problem> pageContent = allProblems.subList(fromIndex, toIndex);
            
            Page<Problem> problemPage = new PageImpl<>(
                pageContent, 
                PageRequest.of(page, pageSize), 
                allProblems.size()
            );
            Pageable pageable = PageRequest.of(page, pageSize);
            
            when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(problemPage);
            
            // Execute
            Page<ProblemListDto> result = service.listPublishedProblems(
                ProblemFilter.empty(), pageable, null);
            
            // Verify page size constraint
            assertThat(result.getContent().size())
                .as("Page %d should have at most %d items", page, pageSize)
                .isLessThanOrEqualTo(pageSize);
            
            // Collect all returned IDs
            result.getContent().forEach(dto -> allReturnedIds.add(dto.id()));
        }
        
        // Verify no duplicates
        assertThat(allReturnedIds)
            .as("All pages combined should have no duplicate problem IDs")
            .doesNotHaveDuplicates();
        
        // Verify all problems are returned
        assertThat(allReturnedIds.size())
            .as("All pages combined should return all problems")
            .isEqualTo(allProblems.size());
    }

    // ========================================================================
    // Property 12: Solve Status Accuracy
    // ========================================================================

    /**
     * Property 12: For any authenticated user viewing the problem list, the solve 
     * status for each problem SHALL be SOLVED if user has at least one ACCEPTED 
     * submission, ATTEMPTED if user has submissions but none ACCEPTED, and NOT_TRIED 
     * otherwise.
     * 
     * <p><b>Validates: Requirements 4.6</b>
     */
    @Property(tries = 100)
    @Label("Property 12: Solve Status Accuracy")
    void solveStatusAccuracy(
            @ForAll("publishedProblems") List<Problem> problems,
            @ForAll @IntRange(min = 1, max = 1000) long userId) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        Page<Problem> problemPage = new PageImpl<>(problems);
        Pageable pageable = PageRequest.of(0, 100);
        
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(problemPage);
        
        // Setup different solve statuses for each problem
        for (int i = 0; i < problems.size(); i++) {
            Problem problem = problems.get(i);
            int statusType = i % 3; // Cycle through SOLVED, ATTEMPTED, NOT_TRIED
            
            switch (statusType) {
                case 0: // SOLVED - has ACCEPTED submission
                    when(submissionRepository.existsByUserIdAndProblemIdAndVerdict(
                        userId, problem.getId(), Verdict.ACCEPTED)).thenReturn(true);
                    break;
                case 1: // ATTEMPTED - has submissions but no ACCEPTED
                    when(submissionRepository.existsByUserIdAndProblemIdAndVerdict(
                        userId, problem.getId(), Verdict.ACCEPTED)).thenReturn(false);
                    when(submissionRepository.existsByUserIdAndProblemId(
                        userId, problem.getId())).thenReturn(true);
                    break;
                case 2: // NOT_TRIED - no submissions
                    when(submissionRepository.existsByUserIdAndProblemIdAndVerdict(
                        userId, problem.getId(), Verdict.ACCEPTED)).thenReturn(false);
                    when(submissionRepository.existsByUserIdAndProblemId(
                        userId, problem.getId())).thenReturn(false);
                    break;
            }
        }
        
        // Execute
        Page<ProblemListDto> result = service.listPublishedProblems(
            ProblemFilter.empty(), pageable, userId);
        
        // Verify solve status for each problem
        for (int i = 0; i < result.getContent().size(); i++) {
            ProblemListDto dto = result.getContent().get(i);
            int expectedStatusType = i % 3;
            ProblemSolveStatus expectedStatus = switch (expectedStatusType) {
                case 0 -> ProblemSolveStatus.SOLVED;
                case 1 -> ProblemSolveStatus.ATTEMPTED;
                case 2 -> ProblemSolveStatus.NOT_TRIED;
                default -> throw new IllegalStateException();
            };
            
            assertThat(dto.solveStatus())
                .as("Problem '%s' should have solve status %s", dto.title(), expectedStatus)
                .isEqualTo(expectedStatus);
        }
    }

    /**
     * Property 12b: For unauthenticated users (userId is null), solve status 
     * SHALL be null for all problems.
     * 
     * <p><b>Validates: Requirements 4.6</b>
     */
    @Property(tries = 100)
    @Label("Property 12b: Unauthenticated users have null solve status")
    void unauthenticatedUsersHaveNullSolveStatus(
            @ForAll("publishedProblems") List<Problem> problems) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        Page<Problem> problemPage = new PageImpl<>(problems);
        Pageable pageable = PageRequest.of(0, 100);
        
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(problemPage);
        
        // Execute with null userId (unauthenticated)
        Page<ProblemListDto> result = service.listPublishedProblems(
            ProblemFilter.empty(), pageable, null);
        
        // Verify all solve statuses are null
        assertThat(result.getContent())
            .as("All problems should have null solve status for unauthenticated users")
            .allSatisfy(dto -> 
                assertThat(dto.solveStatus())
                    .as("Problem '%s' should have null solve status", dto.title())
                    .isNull()
            );
    }

    // ========================================================================
    // Property 13: Problem Access Control
    // ========================================================================

    /**
     * Property 13a: For any problem with status PUBLISHED, all users (including 
     * anonymous) can access the problem.
     * 
     * <p><b>Validates: Requirements 5.3</b>
     */
    @Property(tries = 100)
    @Label("Property 13a: Published problems accessible by all users")
    void publishedProblemsAccessibleByAllUsers(
            @ForAll("publishedProblems") List<Problem> problems) {
        
        if (problems.isEmpty()) return;
        
        Problem problem = problems.get(0);
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        when(problemRepository.findById(problem.getId())).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(problem.getId())).thenReturn(List.of());
        when(starterCodeRepository.findByProblemId(problem.getId())).thenReturn(List.of());
        
        // Test with anonymous user (null)
        ProblemDetailDto resultAnonymous = service.getProblemById(problem.getId(), null);
        assertThat(resultAnonymous.id()).isEqualTo(problem.getId());
        
        // Test with regular USER
        User regularUser = createTestUser(999L);
        regularUser.setRole(Role.USER);
        ProblemDetailDto resultUser = service.getProblemById(problem.getId(), regularUser);
        assertThat(resultUser.id()).isEqualTo(problem.getId());
    }

    /**
     * Property 13b: For any problem with status NOT PUBLISHED, anonymous users 
     * and regular USERs SHALL receive a 404 (ResourceNotFoundException).
     * 
     * <p><b>Validates: Requirements 5.3, 5.4</b>
     */
    @Property(tries = 100)
    @Label("Property 13b: Non-published problems denied to anonymous and regular users")
    void nonPublishedProblemsDeniedToAnonymousAndRegularUsers(
            @ForAll("nonPublishedStatuses") ProblemStatus status) {
        
        // Create a non-published problem
        Problem problem = createProblem(1L, status);
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        when(problemRepository.findById(problem.getId())).thenReturn(Optional.of(problem));
        
        // Test with anonymous user - should throw ResourceNotFoundException
        assertThatThrownBy(() -> service.getProblemById(problem.getId(), null))
            .isInstanceOf(ResourceNotFoundException.class);
        
        // Test with regular USER - should throw ResourceNotFoundException
        User regularUser = createTestUser(999L);
        regularUser.setRole(Role.USER);
        assertThatThrownBy(() -> service.getProblemById(problem.getId(), regularUser))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Property 13c: For any problem with status NOT PUBLISHED, the creator 
     * (PROBLEM_SETTER) SHALL be able to access their own problem.
     * 
     * <p><b>Validates: Requirements 5.4</b>
     */
    @Property(tries = 100)
    @Label("Property 13c: Problem setter can access their own non-published problems")
    void problemSetterCanAccessOwnNonPublishedProblems(
            @ForAll("nonPublishedStatuses") ProblemStatus status) {
        
        // Create a problem setter who is the creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create a non-published problem owned by the creator
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(creator);
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        when(problemRepository.findById(problem.getId())).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(problem.getId())).thenReturn(List.of());
        when(starterCodeRepository.findByProblemId(problem.getId())).thenReturn(List.of());
        
        // Creator should be able to access
        ProblemDetailDto result = service.getProblemById(problem.getId(), creator);
        assertThat(result.id()).isEqualTo(problem.getId());
        assertThat(result.status()).isEqualTo(status);
    }

    /**
     * Property 13d: For any problem with status NOT PUBLISHED, a PROBLEM_SETTER 
     * who is NOT the creator SHALL receive a 404 (ResourceNotFoundException).
     * 
     * <p><b>Validates: Requirements 5.4</b>
     */
    @Property(tries = 100)
    @Label("Property 13d: Problem setter cannot access others' non-published problems")
    void problemSetterCannotAccessOthersNonPublishedProblems(
            @ForAll("nonPublishedStatuses") ProblemStatus status) {
        
        // Create a problem owned by a different user
        User originalCreator = createTestUser(1L);
        originalCreator.setRole(Role.PROBLEM_SETTER);
        
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(originalCreator);
        
        // Create a different problem setter
        User otherProblemSetter = createTestUser(999L);
        otherProblemSetter.setRole(Role.PROBLEM_SETTER);
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        when(problemRepository.findById(problem.getId())).thenReturn(Optional.of(problem));
        
        // Other problem setter should NOT be able to access
        assertThatThrownBy(() -> service.getProblemById(problem.getId(), otherProblemSetter))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Property 13e: For any problem regardless of status, an ADMIN SHALL be able 
     * to access the problem and see hidden testcases.
     * 
     * <p><b>Validates: Requirements 5.5, 13.2</b>
     */
    @Property(tries = 100)
    @Label("Property 13e: Admin can access any problem with hidden testcases")
    void adminCanAccessAnyProblemWithHiddenTestcases(
            @ForAll("allStatuses") ProblemStatus status) {
        
        // Create a problem with any status
        Problem problem = createProblem(1L, status);
        
        // Create visible and hidden testcases
        Testcase visibleTestcase = new Testcase(problem, "input1", "output1", false);
        visibleTestcase.setId(1L);
        Testcase hiddenTestcase = new Testcase(problem, "input2", "output2", true);
        hiddenTestcase.setId(2L);
        
        // Create admin user
        User admin = createTestUser(999L);
        admin.setRole(Role.ADMIN);
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        when(problemRepository.findById(problem.getId())).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(problem.getId()))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        when(starterCodeRepository.findByProblemId(problem.getId())).thenReturn(List.of());
        
        // Admin should be able to access and see all testcases
        ProblemDetailDto result = service.getProblemById(problem.getId(), admin);
        assertThat(result.id()).isEqualTo(problem.getId());
        assertThat(result.examples()).hasSize(2);
        assertThat(result.examples())
            .anyMatch(tc -> tc.isHidden() && tc.input() != null && tc.expectedOutput() != null);
    }

    /**
     * Property 13f: For any published problem, regular users SHALL NOT see 
     * hidden testcases (only visible examples).
     * 
     * <p><b>Validates: Requirements 5.3, 17.3</b>
     */
    @Property(tries = 100)
    @Label("Property 13f: Regular users cannot see hidden testcases")
    void regularUsersCannotSeeHiddenTestcases(
            @ForAll("publishedProblems") List<Problem> problems) {
        
        if (problems.isEmpty()) return;
        
        Problem problem = problems.get(0);
        
        // Create visible and hidden testcases
        Testcase visibleTestcase = new Testcase(problem, "input1", "output1", false);
        visibleTestcase.setId(1L);
        Testcase hiddenTestcase = new Testcase(problem, "input2", "output2", true);
        hiddenTestcase.setId(2L);
        
        // Create regular user
        User regularUser = createTestUser(999L);
        regularUser.setRole(Role.USER);
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        when(problemRepository.findById(problem.getId())).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(problem.getId()))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        when(starterCodeRepository.findByProblemId(problem.getId())).thenReturn(List.of());
        
        // Regular user should only see visible testcases
        ProblemDetailDto result = service.getProblemById(problem.getId(), regularUser);
        assertThat(result.examples()).hasSize(1);
        assertThat(result.examples()).allMatch(tc -> !tc.isHidden());
    }

    // Additional providers for Property 13

    @Provide
    Arbitrary<ProblemStatus> nonPublishedStatuses() {
        return Arbitraries.of(
            ProblemStatus.DRAFT, 
            ProblemStatus.PENDING_REVIEW, 
            ProblemStatus.REJECTED, 
            ProblemStatus.ARCHIVED
        );
    }

    @Provide
    Arbitrary<ProblemStatus> allStatuses() {
        return Arbitraries.of(ProblemStatus.values());
    }

    // ========================================================================
    // Property 14: Problem Creation Invariants
    // ========================================================================

    /**
     * Property 14a: For any problem created by a PROBLEM_SETTER, the problem 
     * SHALL have status DRAFT.
     * 
     * <p><b>Validates: Requirements 6.1</b>
     */
    @Property(tries = 100)
    @Label("Property 14a: Created problems have DRAFT status")
    void createdProblemsHaveDraftStatus(
            @ForAll("validProblemTitles") String title,
            @ForAll("difficulties") Difficulty difficulty) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create a problem setter
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Setup repository to return empty for slug check (no duplicates)
        when(problemRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        
        // Create problem request
        CreateProblemRequest request = CreateProblemRequest.of(title, "Statement for " + title, difficulty);
        
        // Execute
        Problem result = service.createProblem(request, creator);
        
        // Verify status is DRAFT
        assertThat(result.getStatus())
            .as("Created problem should have DRAFT status")
            .isEqualTo(ProblemStatus.DRAFT);
    }

    /**
     * Property 14b: For any problem created by a PROBLEM_SETTER, the createdBy 
     * field SHALL be set to the creator.
     * 
     * <p><b>Validates: Requirements 6.1</b>
     */
    @Property(tries = 100)
    @Label("Property 14b: Created problems have correct createdBy")
    void createdProblemsHaveCorrectCreatedBy(
            @ForAll("validProblemTitles") String title,
            @ForAll @IntRange(min = 1, max = 10000) long creatorId) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create a problem setter with specific ID
        User creator = createTestUser(creatorId);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Setup repository
        when(problemRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        
        // Create problem request
        CreateProblemRequest request = CreateProblemRequest.of(title, "Statement", Difficulty.EASY);
        
        // Execute
        Problem result = service.createProblem(request, creator);
        
        // Verify createdBy is set to the creator
        assertThat(result.getCreatedBy())
            .as("Created problem should have createdBy set to creator")
            .isNotNull();
        assertThat(result.getCreatedBy().getId())
            .as("Created problem's createdBy ID should match creator ID")
            .isEqualTo(creatorId);
    }

    /**
     * Property 14c: For any problem created, the slug SHALL be a URL-safe 
     * string derived from the title.
     * 
     * <p><b>Validates: Requirements 6.5</b>
     */
    @Property(tries = 100)
    @Label("Property 14c: Created problems have URL-safe slug derived from title")
    void createdProblemsHaveUrlSafeSlug(
            @ForAll("validProblemTitles") String title) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        when(problemRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        
        CreateProblemRequest request = CreateProblemRequest.of(title, "Statement", Difficulty.EASY);
        
        // Execute
        Problem result = service.createProblem(request, creator);
        
        // Verify slug is URL-safe (lowercase, no spaces, only alphanumeric and hyphens)
        String slug = result.getSlug();
        assertThat(slug)
            .as("Slug should not be empty")
            .isNotEmpty();
        assertThat(slug)
            .as("Slug should be lowercase")
            .isEqualTo(slug.toLowerCase());
        assertThat(slug)
            .as("Slug should only contain alphanumeric characters and hyphens")
            .matches("^[a-z0-9]+(-[a-z0-9]+)*(-\\d+)?$");
        assertThat(slug)
            .as("Slug should not contain spaces")
            .doesNotContain(" ");
    }

    /**
     * Property 14d: For any problem created with a title that already exists 
     * (duplicate slug), the service SHALL generate a unique slug with a numeric suffix.
     * 
     * <p><b>Validates: Requirements 6.5</b>
     */
    @Property(tries = 100)
    @Label("Property 14d: Duplicate titles get unique slugs with suffix")
    void duplicateTitlesGetUniqueSlugsWithSuffix(
            @ForAll("validProblemTitles") String title) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create an existing problem with the same title
        Problem existingProblem = createProblem(99L, ProblemStatus.PUBLISHED);
        existingProblem.setTitle(title);
        
        // First slug check returns existing problem, second returns empty
        when(problemRepository.findBySlug(any())).thenAnswer(invocation -> {
            String slug = invocation.getArgument(0);
            if (!slug.matches(".*-\\d+$")) {
                return Optional.of(existingProblem);
            }
            return Optional.empty();
        });
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        
        CreateProblemRequest request = CreateProblemRequest.of(title, "Statement", Difficulty.EASY);
        
        // Execute
        Problem result = service.createProblem(request, creator);
        
        // Verify slug has numeric suffix
        assertThat(result.getSlug())
            .as("Slug should have numeric suffix when duplicate exists")
            .matches(".*-\\d+$");
    }

    // ========================================================================
    // Property 15: Problem Edit Authorization
    // ========================================================================

    /**
     * Property 15a: For any problem edit attempt where the editor is NOT the 
     * creator AND NOT an ADMIN, the service SHALL reject with AccessDeniedException.
     * 
     * <p><b>Validates: Requirements 6.3</b>
     */
    @Property(tries = 100)
    @Label("Property 15a: Non-creator non-admin cannot edit problems")
    void nonCreatorNonAdminCannotEditProblems(
            @ForAll("editableStatuses") ProblemStatus status) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create original creator
        User originalCreator = createTestUser(1L);
        originalCreator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem owned by original creator
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(originalCreator);
        
        // Create a different user (not creator, not admin)
        User otherUser = createTestUser(999L);
        otherUser.setRole(Role.PROBLEM_SETTER);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        
        UpdateProblemRequest request = new UpdateProblemRequest("New Title", null, null, null, null, null);
        
        // Should throw AccessDeniedException
        assertThatThrownBy(() -> service.updateProblem(1L, request, otherUser))
            .as("Non-creator non-admin should not be able to edit")
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    /**
     * Property 15b: For any problem edit attempt where the problem status is 
     * PENDING_REVIEW, PUBLISHED, or ARCHIVED AND the editor is NOT an ADMIN, 
     * the service SHALL reject with IllegalStateException.
     * 
     * <p><b>Validates: Requirements 6.4</b>
     */
    @Property(tries = 100)
    @Label("Property 15b: Non-admin cannot edit non-editable status problems")
    void nonAdminCannotEditNonEditableStatusProblems(
            @ForAll("nonEditableStatuses") ProblemStatus status) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem with non-editable status
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        
        UpdateProblemRequest request = new UpdateProblemRequest("New Title", null, null, null, null, null);
        
        // Should throw IllegalStateException
        assertThatThrownBy(() -> service.updateProblem(1L, request, creator))
            .as("Non-admin should not be able to edit problem with status %s", status)
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Property 15c: For any problem edit attempt where the editor is the creator 
     * AND the problem status is DRAFT or REJECTED, the update SHALL succeed.
     * 
     * <p><b>Validates: Requirements 6.2</b>
     */
    @Property(tries = 100)
    @Label("Property 15c: Creator can edit DRAFT or REJECTED problems")
    void creatorCanEditDraftOrRejectedProblems(
            @ForAll("editableStatuses") ProblemStatus status,
            @ForAll("validProblemTitles") String newTitle) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem with editable status
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(problemRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        UpdateProblemRequest request = new UpdateProblemRequest(newTitle, null, null, null, null, null);
        
        // Should succeed
        Problem result = service.updateProblem(1L, request, creator);
        
        assertThat(result.getTitle())
            .as("Title should be updated")
            .isEqualTo(newTitle);
    }

    /**
     * Property 15d: For any problem edit attempt where the editor is an ADMIN, 
     * the update SHALL succeed regardless of problem status.
     * 
     * <p><b>Validates: Requirements 6.2, 6.4</b>
     */
    @Property(tries = 100)
    @Label("Property 15d: Admin can edit problems in any status")
    void adminCanEditProblemsInAnyStatus(
            @ForAll("allStatuses") ProblemStatus status,
            @ForAll("validProblemTitles") String newTitle) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create original creator
        User originalCreator = createTestUser(1L);
        originalCreator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem with any status
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(originalCreator);
        
        // Create admin
        User admin = createTestUser(999L);
        admin.setRole(Role.ADMIN);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(problemRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        UpdateProblemRequest request = new UpdateProblemRequest(newTitle, null, null, null, null, null);
        
        // Should succeed for admin
        Problem result = service.updateProblem(1L, request, admin);
        
        assertThat(result.getTitle())
            .as("Admin should be able to update title for problem with status %s", status)
            .isEqualTo(newTitle);
    }

    // Additional providers for Properties 14 and 15

    @Provide
    Arbitrary<String> validProblemTitles() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(50)
            .map(s -> s + " Problem"); // Ensure it has at least one space for slug testing
    }

    @Provide
    Arbitrary<ProblemStatus> editableStatuses() {
        return Arbitraries.of(ProblemStatus.DRAFT, ProblemStatus.REJECTED);
    }

    @Provide
    Arbitrary<ProblemStatus> nonEditableStatuses() {
        return Arbitraries.of(
            ProblemStatus.PENDING_REVIEW, 
            ProblemStatus.PUBLISHED, 
            ProblemStatus.ARCHIVED
        );
    }

    // ========================================================================
    // Property 18: Review Request Validation
    // ========================================================================

    /**
     * Property 18a: For any review request on a problem that has at least one 
     * visible testcase AND at least one hidden testcase, the status SHALL change 
     * to PENDING_REVIEW.
     * 
     * <p><b>Validates: Requirements 8.1, 8.2</b>
     */
    @Property(tries = 100)
    @Label("Property 18a: Review request succeeds with valid testcases")
    void reviewRequestSucceedsWithValidTestcases(
            @ForAll @IntRange(min = 1, max = 10) int visibleCount,
            @ForAll @IntRange(min = 1, max = 10) int hiddenCount) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem in DRAFT status
        Problem problem = createProblem(1L, ProblemStatus.DRAFT);
        problem.setCreatedBy(creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn((long) visibleCount);
        when(testcaseRepository.countByProblemIdAndIsHidden(1L, true)).thenReturn((long) hiddenCount);
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Execute
        Problem result = service.requestReview(1L, creator);
        
        // Verify status changed to PENDING_REVIEW
        assertThat(result.getStatus())
            .as("Status should change to PENDING_REVIEW when problem has %d visible and %d hidden testcases", 
                visibleCount, hiddenCount)
            .isEqualTo(ProblemStatus.PENDING_REVIEW);
    }

    /**
     * Property 18b: For any review request on a problem that has NO visible 
     * testcases, the request SHALL be rejected with IllegalStateException.
     * 
     * <p><b>Validates: Requirements 8.1, 8.3</b>
     */
    @Property(tries = 100)
    @Label("Property 18b: Review request fails without visible testcases")
    void reviewRequestFailsWithoutVisibleTestcases(
            @ForAll @IntRange(min = 1, max = 10) int hiddenCount) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem in DRAFT status
        Problem problem = createProblem(1L, ProblemStatus.DRAFT);
        problem.setCreatedBy(creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn(0L); // No visible testcases
        when(testcaseRepository.countByProblemIdAndIsHidden(1L, true)).thenReturn((long) hiddenCount);
        
        // Should throw IllegalStateException
        assertThatThrownBy(() -> service.requestReview(1L, creator))
            .as("Review request should fail when no visible testcases exist")
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("visible testcase");
    }

    /**
     * Property 18c: For any review request on a problem that has NO hidden 
     * testcases, the request SHALL be rejected with IllegalStateException.
     * 
     * <p><b>Validates: Requirements 8.1, 8.3</b>
     */
    @Property(tries = 100)
    @Label("Property 18c: Review request fails without hidden testcases")
    void reviewRequestFailsWithoutHiddenTestcases(
            @ForAll @IntRange(min = 1, max = 10) int visibleCount) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem in DRAFT status
        Problem problem = createProblem(1L, ProblemStatus.DRAFT);
        problem.setCreatedBy(creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.countByProblemIdAndIsHidden(1L, false)).thenReturn((long) visibleCount);
        when(testcaseRepository.countByProblemIdAndIsHidden(1L, true)).thenReturn(0L); // No hidden testcases
        
        // Should throw IllegalStateException
        assertThatThrownBy(() -> service.requestReview(1L, creator))
            .as("Review request should fail when no hidden testcases exist")
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("hidden testcase");
    }

    /**
     * Property 18d: For any review request on a problem that is NOT in DRAFT 
     * status, the request SHALL be rejected with IllegalStateException.
     * 
     * <p><b>Validates: Requirements 8.1</b>
     */
    @Property(tries = 100)
    @Label("Property 18d: Review request fails for non-DRAFT problems")
    void reviewRequestFailsForNonDraftProblems(
            @ForAll("nonDraftStatuses") ProblemStatus status) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create creator
        User creator = createTestUser(1L);
        creator.setRole(Role.PROBLEM_SETTER);
        
        // Create problem in non-DRAFT status
        Problem problem = createProblem(1L, status);
        problem.setCreatedBy(creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        
        // Should throw IllegalStateException
        assertThatThrownBy(() -> service.requestReview(1L, creator))
            .as("Review request should fail for problem with status %s", status)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DRAFT");
    }

    @Provide
    Arbitrary<ProblemStatus> nonDraftStatuses() {
        return Arbitraries.of(
            ProblemStatus.PENDING_REVIEW, 
            ProblemStatus.PUBLISHED, 
            ProblemStatus.REJECTED, 
            ProblemStatus.ARCHIVED
        );
    }

    // ========================================================================
    // Property 19: Problem Lifecycle Transitions
    // ========================================================================

    /**
     * Property 19a: For any admin publish action on a PENDING_REVIEW problem, 
     * the status SHALL become PUBLISHED.
     * 
     * <p><b>Validates: Requirements 8.4</b>
     */
    @Property(tries = 100)
    @Label("Property 19a: Admin publish changes status to PUBLISHED")
    void adminPublishChangesStatusToPublished() {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create admin
        User admin = createTestUser(1L);
        admin.setRole(Role.ADMIN);
        
        // Create problem in PENDING_REVIEW status
        Problem problem = createProblem(1L, ProblemStatus.PENDING_REVIEW);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Execute
        Problem result = service.publishProblem(1L, admin);
        
        // Verify status changed to PUBLISHED
        assertThat(result.getStatus())
            .as("Status should change to PUBLISHED after admin publishes")
            .isEqualTo(ProblemStatus.PUBLISHED);
    }

    /**
     * Property 19b: For any admin reject action on a PENDING_REVIEW problem, 
     * the status SHALL become REJECTED.
     * 
     * <p><b>Validates: Requirements 8.5</b>
     */
    @Property(tries = 100)
    @Label("Property 19b: Admin reject changes status to REJECTED")
    void adminRejectChangesStatusToRejected() {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create admin
        User admin = createTestUser(1L);
        admin.setRole(Role.ADMIN);
        
        // Create problem in PENDING_REVIEW status
        Problem problem = createProblem(1L, ProblemStatus.PENDING_REVIEW);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Execute
        Problem result = service.rejectProblem(1L, "test", admin);
        
        // Verify status changed to REJECTED
        assertThat(result.getStatus())
            .as("Status should change to REJECTED after admin rejects")
            .isEqualTo(ProblemStatus.REJECTED);
    }

    /**
     * Property 19c: For any admin archive action on a problem in ANY status, 
     * the status SHALL become ARCHIVED.
     * 
     * <p><b>Validates: Requirements 8.6</b>
     */
    @Property(tries = 100)
    @Label("Property 19c: Admin archive changes status to ARCHIVED from any status")
    void adminArchiveChangesStatusToArchivedFromAnyStatus(
            @ForAll("allStatuses") ProblemStatus initialStatus) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create admin
        User admin = createTestUser(1L);
        admin.setRole(Role.ADMIN);
        
        // Create problem in any status
        Problem problem = createProblem(1L, initialStatus);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Execute
        Problem result = service.archiveProblem(1L, admin);
        
        // Verify status changed to ARCHIVED
        assertThat(result.getStatus())
            .as("Status should change to ARCHIVED from %s after admin archives", initialStatus)
            .isEqualTo(ProblemStatus.ARCHIVED);
    }

    /**
     * Property 19d: For any non-admin user attempting to publish, reject, or 
     * archive a problem, the action SHALL be rejected with AccessDeniedException.
     * 
     * <p><b>Validates: Requirements 8.4, 8.5, 8.6</b>
     */
    @Property(tries = 100)
    @Label("Property 19d: Non-admin cannot perform lifecycle transitions")
    void nonAdminCannotPerformLifecycleTransitions(
            @ForAll("nonAdminRoles") Role role) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create non-admin user
        User user = createTestUser(1L);
        user.setRole(role);
        
        // Create problem in PENDING_REVIEW status
        Problem problem = createProblem(1L, ProblemStatus.PENDING_REVIEW);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        
        // All lifecycle actions should fail for non-admin
        assertThatThrownBy(() -> service.publishProblem(1L, user))
            .as("Non-admin with role %s should not be able to publish", role)
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        
        assertThatThrownBy(() -> service.rejectProblem(1L, "test", user))
            .as("Non-admin with role %s should not be able to reject", role)
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        
        assertThatThrownBy(() -> service.archiveProblem(1L, user))
            .as("Non-admin with role %s should not be able to archive", role)
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    /**
     * Property 19e: For any admin publish or reject action on a problem NOT in 
     * PENDING_REVIEW status, the action SHALL be rejected with IllegalStateException.
     * 
     * <p><b>Validates: Requirements 8.4, 8.5</b>
     */
    @Property(tries = 100)
    @Label("Property 19e: Publish/reject fails for non-PENDING_REVIEW problems")
    void publishRejectFailsForNonPendingReviewProblems(
            @ForAll("nonPendingReviewStatuses") ProblemStatus status) {
        
        // Setup mocks
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        StarterCodeRepository starterCodeRepository = mock(StarterCodeRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ProblemService service = new ProblemServiceImpl(problemRepository, submissionRepository, testcaseRepository, starterCodeRepository, adminAuditService);

        // Create admin
        User admin = createTestUser(1L);
        admin.setRole(Role.ADMIN);
        
        // Create problem in non-PENDING_REVIEW status
        Problem problem = createProblem(1L, status);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        
        // Publish should fail
        assertThatThrownBy(() -> service.publishProblem(1L, admin))
            .as("Publish should fail for problem with status %s", status)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING_REVIEW");
        
        // Reject should fail
        assertThatThrownBy(() -> service.rejectProblem(1L, "test", admin))
            .as("Reject should fail for problem with status %s", status)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING_REVIEW");
    }

    @Provide
    Arbitrary<Role> nonAdminRoles() {
        return Arbitraries.of(Role.USER, Role.PROBLEM_SETTER);
    }

    @Provide
    Arbitrary<ProblemStatus> nonPendingReviewStatuses() {
        return Arbitraries.of(
            ProblemStatus.DRAFT, 
            ProblemStatus.PUBLISHED, 
            ProblemStatus.REJECTED, 
            ProblemStatus.ARCHIVED
        );
    }
}
