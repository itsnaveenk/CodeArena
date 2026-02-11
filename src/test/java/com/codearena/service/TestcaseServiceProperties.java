package com.codearena.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;

import com.codearena.dto.CreateTestcaseRequest;
import com.codearena.dto.TestcaseDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.TestcaseRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based tests for TestcaseService operations.
 * 
 * <p>These tests verify the correctness properties defined in the design document:
 * <ul>
 *   <li>Property 16: Testcase Management Authorization</li>
 *   <li>Property 17: Hidden Testcase Protection</li>
 * </ul>
 * 
 * <p><b>Validates: Requirements 7.1-7.5, 17.3</b>
 */
@Label("TestcaseService Property Tests")
@Tag("codearena-platform")
class TestcaseServiceProperties {

    // ========================================================================
    // Test Data Generators
    // ========================================================================

    @Provide
    Arbitrary<String> testcaseInputs() {
        return Arbitraries.strings()
            .ofMinLength(1)
            .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> testcaseOutputs() {
        return Arbitraries.strings()
            .ofMinLength(1)
            .ofMaxLength(100);
    }

    @Provide
    Arbitrary<Boolean> hiddenFlags() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<Role> nonAdminRoles() {
        return Arbitraries.of(Role.USER, Role.PROBLEM_SETTER);
    }

    private User createUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(role);
        return user;
    }

    private Problem createProblem(Long id, User creator) {
        Problem problem = new Problem(
            "Problem " + id,
            "problem-" + id,
            "Statement for problem " + id,
            Difficulty.EASY,
            creator
        );
        problem.setId(id);
        problem.setStatus(ProblemStatus.DRAFT);
        // Ensure createdBy is set correctly (constructor should do this, but be explicit)
        problem.setCreatedBy(creator);
        return problem;
    }

    private Testcase createTestcase(Long id, Problem problem, boolean isHidden) {
        Testcase testcase = new Testcase(
            problem,
            "input-" + id,
            "output-" + id,
            isHidden
        );
        testcase.setId(id);
        return testcase;
    }

    // ========================================================================
    // Property 16: Testcase Management Authorization
    // ========================================================================

    /**
     * Property 16a: For any testcase add operation, if the operator is the 
     * problem creator, the operation SHALL succeed.
     * 
     * <p><b>Validates: Requirements 7.1</b>
     */
    @Property(tries = 100)
    @Label("Property 16a: Problem creator can add testcases")
    void problemCreatorCanAddTestcases(
            @ForAll("testcaseInputs") String input,
            @ForAll("testcaseOutputs") String output,
            @ForAll("hiddenFlags") boolean isHidden,
            @ForAll @IntRange(min = 1, max = 1000) long creatorId) {
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        Problem problem = createProblem(1L, creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> {
            Testcase tc = inv.getArgument(0);
            tc.setId(1L);
            return tc;
        });
        
        CreateTestcaseRequest request = new CreateTestcaseRequest(input, output, isHidden);
        
        // Execute
        Testcase result = service.addTestcase(1L, request, creator);
        
        // Verify
        assertThat(result).isNotNull();
        assertThat(result.getInput()).isEqualTo(input);
        assertThat(result.getExpectedOutput()).isEqualTo(output);
        assertThat(result.isHidden()).isEqualTo(isHidden);
        verify(testcaseRepository).save(any(Testcase.class));
    }

    /**
     * Property 16b: For any testcase add operation, if the operator is an ADMIN,
     * the operation SHALL succeed regardless of problem ownership.
     * 
     * <p><b>Validates: Requirements 7.5</b>
     */
    @Property(tries = 100)
    @Label("Property 16b: Admin can add testcases to any problem")
    void adminCanAddTestcasesToAnyProblem(
            @ForAll("testcaseInputs") String input,
            @ForAll("testcaseOutputs") String output,
            @ForAll("hiddenFlags") boolean isHidden,
            @ForAll @IntRange(min = 1, max = 1000) long creatorId,
            @ForAll @IntRange(min = 1001, max = 2000) long adminId) {
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User admin = createUser(adminId, Role.ADMIN);
        Problem problem = createProblem(1L, creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> {
            Testcase tc = inv.getArgument(0);
            tc.setId(1L);
            return tc;
        });
        
        CreateTestcaseRequest request = new CreateTestcaseRequest(input, output, isHidden);
        
        // Execute - admin adding to someone else's problem
        Testcase result = service.addTestcase(1L, request, admin);
        
        // Verify
        assertThat(result).isNotNull();
        verify(testcaseRepository).save(any(Testcase.class));
    }

    /**
     * Property 16c: For any testcase add operation, if the operator is NOT the 
     * problem creator AND NOT an ADMIN, the operation SHALL be rejected with 403.
     * 
     * <p><b>Validates: Requirements 7.3</b>
     */
    @Property(tries = 100)
    @Label("Property 16c: Non-creator non-admin cannot add testcases")
    void nonCreatorNonAdminCannotAddTestcases(
            @ForAll("nonAdminRoles") Role operatorRole) {
        
        // Use fixed IDs to ensure they're different
        long creatorId = 1L;
        long operatorId = 999L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User operator = createUser(operatorId, operatorRole);
        Problem problem = createProblem(1L, creator);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        // Don't mock save - it should never be called
        
        CreateTestcaseRequest request = new CreateTestcaseRequest("input", "output", false);
        
        // Execute & Verify
        assertThatThrownBy(() -> service.addTestcase(1L, request, operator))
            .isInstanceOf(AccessDeniedException.class);
        
        verify(testcaseRepository, never()).save(any());
    }

    /**
     * Property 16d: For any testcase delete operation, if the operator is the 
     * problem creator, the operation SHALL succeed.
     * 
     * <p><b>Validates: Requirements 7.4</b>
     */
    @Property(tries = 100)
    @Label("Property 16d: Problem creator can delete testcases")
    void problemCreatorCanDeleteTestcases(
            @ForAll("hiddenFlags") boolean isHidden,
            @ForAll @IntRange(min = 1, max = 1000) long creatorId) {
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        Problem problem = createProblem(1L, creator);
        Testcase testcase = createTestcase(1L, problem, isHidden);
        
        when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));
        
        // Execute
        service.deleteTestcase(1L, creator);
        
        // Verify
        verify(testcaseRepository).delete(testcase);
    }

    /**
     * Property 16e: For any testcase delete operation, if the operator is an ADMIN,
     * the operation SHALL succeed regardless of problem ownership.
     * 
     * <p><b>Validates: Requirements 7.5</b>
     */
    @Property(tries = 100)
    @Label("Property 16e: Admin can delete testcases from any problem")
    void adminCanDeleteTestcasesFromAnyProblem(
            @ForAll("hiddenFlags") boolean isHidden,
            @ForAll @IntRange(min = 1, max = 1000) long creatorId,
            @ForAll @IntRange(min = 1001, max = 2000) long adminId) {
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User admin = createUser(adminId, Role.ADMIN);
        Problem problem = createProblem(1L, creator);
        Testcase testcase = createTestcase(1L, problem, isHidden);
        
        when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));
        
        // Execute - admin deleting from someone else's problem
        service.deleteTestcase(1L, admin);
        
        // Verify
        verify(testcaseRepository).delete(testcase);
    }

    /**
     * Property 16f: For any testcase delete operation, if the operator is NOT the 
     * problem creator AND NOT an ADMIN, the operation SHALL be rejected with 403.
     * 
     * <p><b>Validates: Requirements 7.3</b>
     */
    @Property(tries = 100)
    @Label("Property 16f: Non-creator non-admin cannot delete testcases")
    void nonCreatorNonAdminCannotDeleteTestcases(
            @ForAll("nonAdminRoles") Role operatorRole) {
        
        // Use fixed IDs to ensure they're different
        long creatorId = 1L;
        long operatorId = 999L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User operator = createUser(operatorId, operatorRole);
        Problem problem = createProblem(1L, creator);
        Testcase testcase = createTestcase(1L, problem, false);
        
        when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));
        
        // Execute & Verify
        assertThatThrownBy(() -> service.deleteTestcase(1L, operator))
            .isInstanceOf(AccessDeniedException.class);
        
        verify(testcaseRepository, never()).delete(any());
    }

    // ========================================================================
    // Property 17: Hidden Testcase Protection
    // ========================================================================

    /**
     * Property 17a: For any API response to a user with role USER, hidden testcase 
     * inputs and expected outputs SHALL NOT be present in the response.
     * 
     * <p><b>Validates: Requirements 7.2, 17.3</b>
     */
    @Property(tries = 100)
    @Label("Property 17a: Regular users cannot see hidden testcase details")
    void regularUsersCannotSeeHiddenTestcaseDetails() {
        
        // Use fixed IDs to ensure they're different
        long creatorId = 1L;
        long userId = 999L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User regularUser = createUser(userId, Role.USER);
        Problem problem = createProblem(1L, creator);
        
        Testcase visibleTestcase = createTestcase(1L, problem, false);
        Testcase hiddenTestcase = createTestcase(2L, problem, true);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(1L))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        
        // Execute
        List<TestcaseDto> result = service.getTestcases(1L, regularUser);
        
        // Verify - hidden testcases should not be in the result
        assertThat(result)
            .as("Regular users should not see hidden testcases")
            .noneMatch(TestcaseDto::isHidden);
        
        // Verify visible testcases are present
        assertThat(result)
            .as("Regular users should see visible testcases")
            .anyMatch(tc -> !tc.isHidden());
    }

    /**
     * Property 17b: For any API response to a PROBLEM_SETTER viewing another's 
     * problem, hidden testcase inputs and expected outputs SHALL NOT be present.
     * 
     * <p><b>Validates: Requirements 7.2, 17.3</b>
     */
    @Property(tries = 100)
    @Label("Property 17b: Problem setters cannot see others' hidden testcases")
    void problemSettersCannotSeeOthersHiddenTestcases() {
        
        // Use fixed IDs to ensure they're different
        long creatorId = 1L;
        long otherSetterId = 999L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User otherSetter = createUser(otherSetterId, Role.PROBLEM_SETTER);
        Problem problem = createProblem(1L, creator);
        
        Testcase visibleTestcase = createTestcase(1L, problem, false);
        Testcase hiddenTestcase = createTestcase(2L, problem, true);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(1L))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        
        // Execute
        List<TestcaseDto> result = service.getTestcases(1L, otherSetter);
        
        // Verify - hidden testcases should not be in the result
        assertThat(result)
            .as("Other problem setters should not see hidden testcases")
            .noneMatch(TestcaseDto::isHidden);
    }

    /**
     * Property 17c: For any API response to the problem creator, hidden testcase 
     * inputs and expected outputs SHALL be present with full details.
     * 
     * <p><b>Validates: Requirements 7.2</b>
     */
    @Property(tries = 100)
    @Label("Property 17c: Problem creator can see their own hidden testcases")
    void problemCreatorCanSeeOwnHiddenTestcases() {
        
        long creatorId = 1L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        Problem problem = createProblem(1L, creator);
        
        Testcase visibleTestcase = createTestcase(1L, problem, false);
        Testcase hiddenTestcase = createTestcase(2L, problem, true);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(1L))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        
        // Execute
        List<TestcaseDto> result = service.getTestcases(1L, creator);
        
        // Verify - both visible and hidden testcases should be present
        assertThat(result).hasSize(2);
        
        // Verify hidden testcase has full details
        TestcaseDto hiddenDto = result.stream()
            .filter(TestcaseDto::isHidden)
            .findFirst()
            .orElseThrow();
        
        assertThat(hiddenDto.input())
            .as("Creator should see hidden testcase input")
            .isNotNull()
            .isEqualTo("input-2");
        assertThat(hiddenDto.expectedOutput())
            .as("Creator should see hidden testcase expected output")
            .isNotNull()
            .isEqualTo("output-2");
    }

    /**
     * Property 17d: For any API response to an ADMIN, hidden testcase inputs 
     * and expected outputs SHALL be present with full details.
     * 
     * <p><b>Validates: Requirements 7.5, 17.3</b>
     */
    @Property(tries = 100)
    @Label("Property 17d: Admin can see all hidden testcases")
    void adminCanSeeAllHiddenTestcases() {
        
        // Use fixed IDs to ensure they're different
        long creatorId = 1L;
        long adminId = 999L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        User admin = createUser(adminId, Role.ADMIN);
        Problem problem = createProblem(1L, creator);
        
        Testcase visibleTestcase = createTestcase(1L, problem, false);
        Testcase hiddenTestcase = createTestcase(2L, problem, true);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(1L))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        
        // Execute
        List<TestcaseDto> result = service.getTestcases(1L, admin);
        
        // Verify - both visible and hidden testcases should be present
        assertThat(result).hasSize(2);
        
        // Verify hidden testcase has full details
        TestcaseDto hiddenDto = result.stream()
            .filter(TestcaseDto::isHidden)
            .findFirst()
            .orElseThrow();
        
        assertThat(hiddenDto.input())
            .as("Admin should see hidden testcase input")
            .isNotNull()
            .isEqualTo("input-2");
        assertThat(hiddenDto.expectedOutput())
            .as("Admin should see hidden testcase expected output")
            .isNotNull()
            .isEqualTo("output-2");
    }

    /**
     * Property 17e: For any unauthenticated request (null user), hidden testcase 
     * inputs and expected outputs SHALL NOT be present.
     * 
     * <p><b>Validates: Requirements 7.2, 17.3</b>
     */
    @Property(tries = 100)
    @Label("Property 17e: Unauthenticated users cannot see hidden testcases")
    void unauthenticatedUsersCannotSeeHiddenTestcases() {
        
        long creatorId = 1L;
        
        // Setup
        TestcaseRepository testcaseRepository = mock(TestcaseRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        TestcaseService service = new TestcaseServiceImpl(testcaseRepository, problemRepository);
        
        User creator = createUser(creatorId, Role.PROBLEM_SETTER);
        Problem problem = createProblem(1L, creator);
        
        Testcase visibleTestcase = createTestcase(1L, problem, false);
        Testcase hiddenTestcase = createTestcase(2L, problem, true);
        
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testcaseRepository.findByProblemId(1L))
            .thenReturn(List.of(visibleTestcase, hiddenTestcase));
        
        // Execute with null user
        List<TestcaseDto> result = service.getTestcases(1L, null);
        
        // Verify - hidden testcases should not be in the result
        assertThat(result)
            .as("Unauthenticated users should not see hidden testcases")
            .noneMatch(TestcaseDto::isHidden);
        
        // Verify only visible testcases are present
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isHidden()).isFalse();
    }
}
