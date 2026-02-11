package com.codearena.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.codearena.dto.CreateTestcaseRequest;
import com.codearena.dto.TestcaseDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.TestcaseRepository;

/**
 * Unit tests for TestcaseServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestcaseService Tests")
class TestcaseServiceTest {

    @Mock
    private TestcaseRepository testcaseRepository;

    @Mock
    private ProblemRepository problemRepository;

    private TestcaseService testcaseService;

    private User problemSetter;
    private User otherProblemSetter;
    private User admin;
    private User regularUser;
    private Problem problem;

    @BeforeEach
    void setUp() {
        testcaseService = new TestcaseServiceImpl(testcaseRepository, problemRepository);

        // Create test users
        problemSetter = new User();
        problemSetter.setId(1L);
        problemSetter.setName("Problem Setter");
        problemSetter.setEmail("setter@example.com");
        problemSetter.setRole(Role.PROBLEM_SETTER);

        otherProblemSetter = new User();
        otherProblemSetter.setId(2L);
        otherProblemSetter.setName("Other Setter");
        otherProblemSetter.setEmail("other@example.com");
        otherProblemSetter.setRole(Role.PROBLEM_SETTER);

        admin = new User();
        admin.setId(3L);
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        regularUser = new User();
        regularUser.setId(4L);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@example.com");
        regularUser.setRole(Role.USER);

        // Create test problem
        problem = new Problem("Test Problem", "test-problem", "Statement", Difficulty.EASY, problemSetter);
        problem.setId(1L);
        problem.setStatus(ProblemStatus.DRAFT);
    }

    @Nested
    @DisplayName("addTestcase")
    class AddTestcaseTests {

        @Test
        @DisplayName("should add visible testcase when creator adds to own problem")
        void shouldAddVisibleTestcaseWhenCreatorAddsToOwnProblem() {
            CreateTestcaseRequest request = CreateTestcaseRequest.visible("input1", "output1");
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> {
                Testcase tc = inv.getArgument(0);
                tc.setId(1L);
                return tc;
            });

            Testcase result = testcaseService.addTestcase(1L, request, problemSetter);

            assertThat(result.getInput()).isEqualTo("input1");
            assertThat(result.getExpectedOutput()).isEqualTo("output1");
            assertThat(result.isHidden()).isFalse();
            assertThat(result.getProblem()).isEqualTo(problem);
            verify(testcaseRepository).save(any(Testcase.class));
        }

        @Test
        @DisplayName("should add hidden testcase when creator adds to own problem")
        void shouldAddHiddenTestcaseWhenCreatorAddsToOwnProblem() {
            CreateTestcaseRequest request = CreateTestcaseRequest.hidden("input2", "output2");
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> {
                Testcase tc = inv.getArgument(0);
                tc.setId(2L);
                return tc;
            });

            Testcase result = testcaseService.addTestcase(1L, request, problemSetter);

            assertThat(result.getInput()).isEqualTo("input2");
            assertThat(result.getExpectedOutput()).isEqualTo("output2");
            assertThat(result.isHidden()).isTrue();
        }

        @Test
        @DisplayName("should add testcase when admin adds to any problem")
        void shouldAddTestcaseWhenAdminAddsToAnyProblem() {
            CreateTestcaseRequest request = CreateTestcaseRequest.visible("input", "output");
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> {
                Testcase tc = inv.getArgument(0);
                tc.setId(1L);
                return tc;
            });

            Testcase result = testcaseService.addTestcase(1L, request, admin);

            assertThat(result).isNotNull();
            verify(testcaseRepository).save(any(Testcase.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when other problem setter tries to add")
        void shouldThrowAccessDeniedWhenOtherProblemSetterTriesToAdd() {
            CreateTestcaseRequest request = CreateTestcaseRequest.visible("input", "output");
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            assertThatThrownBy(() -> testcaseService.addTestcase(1L, request, otherProblemSetter))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");

            verify(testcaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when regular user tries to add")
        void shouldThrowAccessDeniedWhenRegularUserTriesToAdd() {
            CreateTestcaseRequest request = CreateTestcaseRequest.visible("input", "output");
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

            assertThatThrownBy(() -> testcaseService.addTestcase(1L, request, regularUser))
                .isInstanceOf(AccessDeniedException.class);

            verify(testcaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when problem not found")
        void shouldThrowResourceNotFoundWhenProblemNotFound() {
            CreateTestcaseRequest request = CreateTestcaseRequest.visible("input", "output");
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testcaseService.addTestcase(999L, request, problemSetter))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Problem not found");
        }
    }

    @Nested
    @DisplayName("getTestcases")
    class GetTestcasesTests {

        private Testcase visibleTestcase;
        private Testcase hiddenTestcase;

        @BeforeEach
        void setUp() {
            visibleTestcase = new Testcase(problem, "visible-input", "visible-output", false);
            visibleTestcase.setId(1L);

            hiddenTestcase = new Testcase(problem, "hidden-input", "hidden-output", true);
            hiddenTestcase.setId(2L);
        }

        @Test
        @DisplayName("should return all testcases with full details for admin")
        void shouldReturnAllTestcasesWithFullDetailsForAdmin() {
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L))
                .thenReturn(List.of(visibleTestcase, hiddenTestcase));

            List<TestcaseDto> result = testcaseService.getTestcases(1L, admin);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(tc -> tc.id().equals(1L) && !tc.isHidden() 
                && tc.input().equals("visible-input"));
            assertThat(result).anyMatch(tc -> tc.id().equals(2L) && tc.isHidden() 
                && tc.input().equals("hidden-input"));
        }

        @Test
        @DisplayName("should return all testcases with full details for problem creator")
        void shouldReturnAllTestcasesWithFullDetailsForProblemCreator() {
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L))
                .thenReturn(List.of(visibleTestcase, hiddenTestcase));

            List<TestcaseDto> result = testcaseService.getTestcases(1L, problemSetter);

            assertThat(result).hasSize(2);
            // Creator should see hidden testcase details
            TestcaseDto hiddenDto = result.stream()
                .filter(TestcaseDto::isHidden)
                .findFirst()
                .orElseThrow();
            assertThat(hiddenDto.input()).isEqualTo("hidden-input");
            assertThat(hiddenDto.expectedOutput()).isEqualTo("hidden-output");
        }

        @Test
        @DisplayName("should return only visible testcases for regular user")
        void shouldReturnOnlyVisibleTestcasesForRegularUser() {
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L))
                .thenReturn(List.of(visibleTestcase, hiddenTestcase));

            List<TestcaseDto> result = testcaseService.getTestcases(1L, regularUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isHidden()).isFalse();
            assertThat(result.get(0).input()).isEqualTo("visible-input");
        }

        @Test
        @DisplayName("should return only visible testcases for other problem setter")
        void shouldReturnOnlyVisibleTestcasesForOtherProblemSetter() {
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L))
                .thenReturn(List.of(visibleTestcase, hiddenTestcase));

            List<TestcaseDto> result = testcaseService.getTestcases(1L, otherProblemSetter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isHidden()).isFalse();
        }

        @Test
        @DisplayName("should return only visible testcases for null user")
        void shouldReturnOnlyVisibleTestcasesForNullUser() {
            when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
            when(testcaseRepository.findByProblemId(1L))
                .thenReturn(List.of(visibleTestcase, hiddenTestcase));

            List<TestcaseDto> result = testcaseService.getTestcases(1L, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isHidden()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when problem not found")
        void shouldThrowResourceNotFoundWhenProblemNotFound() {
            when(problemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testcaseService.getTestcases(999L, admin))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteTestcase")
    class DeleteTestcaseTests {

        private Testcase testcase;

        @BeforeEach
        void setUp() {
            testcase = new Testcase(problem, "input", "output", false);
            testcase.setId(1L);
        }

        @Test
        @DisplayName("should delete testcase when creator deletes from own problem")
        void shouldDeleteTestcaseWhenCreatorDeletesFromOwnProblem() {
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));

            testcaseService.deleteTestcase(1L, problemSetter);

            verify(testcaseRepository).delete(testcase);
        }

        @Test
        @DisplayName("should delete testcase when admin deletes from any problem")
        void shouldDeleteTestcaseWhenAdminDeletesFromAnyProblem() {
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));

            testcaseService.deleteTestcase(1L, admin);

            verify(testcaseRepository).delete(testcase);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when other problem setter tries to delete")
        void shouldThrowAccessDeniedWhenOtherProblemSetterTriesToDelete() {
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));

            assertThatThrownBy(() -> testcaseService.deleteTestcase(1L, otherProblemSetter))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");

            verify(testcaseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when regular user tries to delete")
        void shouldThrowAccessDeniedWhenRegularUserTriesToDelete() {
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));

            assertThatThrownBy(() -> testcaseService.deleteTestcase(1L, regularUser))
                .isInstanceOf(AccessDeniedException.class);

            verify(testcaseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when testcase not found")
        void shouldThrowResourceNotFoundWhenTestcaseNotFound() {
            when(testcaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testcaseService.deleteTestcase(999L, problemSetter))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Testcase not found");
        }
    }

    @Nested
    @DisplayName("getAllTestcasesForExecution")
    class GetAllTestcasesForExecutionTests {

        @Test
        @DisplayName("should return all testcases including hidden ones")
        void shouldReturnAllTestcasesIncludingHiddenOnes() {
            Testcase visible = new Testcase(problem, "v-input", "v-output", false);
            visible.setId(1L);
            Testcase hidden = new Testcase(problem, "h-input", "h-output", true);
            hidden.setId(2L);

            when(testcaseRepository.findByProblemId(1L)).thenReturn(List.of(visible, hidden));

            List<Testcase> result = testcaseService.getAllTestcasesForExecution(1L);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(tc -> !tc.isHidden());
            assertThat(result).anyMatch(Testcase::isHidden);
        }
    }

    @Nested
    @DisplayName("getVisibleTestcases")
    class GetVisibleTestcasesTests {

        @Test
        @DisplayName("should return only visible testcases")
        void shouldReturnOnlyVisibleTestcases() {
            Testcase visible = new Testcase(problem, "v-input", "v-output", false);
            visible.setId(1L);

            when(testcaseRepository.findByProblemIdAndIsHidden(1L, false))
                .thenReturn(List.of(visible));

            List<Testcase> result = testcaseService.getVisibleTestcases(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isHidden()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateTestcase")
    class UpdateTestcaseTests {

        private Testcase testcase;

        @BeforeEach
        void setUp() {
            testcase = new Testcase(problem, "old-input", "old-output", false);
            testcase.setId(1L);
        }

        @Test
        @DisplayName("should update testcase when creator updates own problem")
        void shouldUpdateTestcaseWhenCreatorUpdatesOwnProblem() {
            CreateTestcaseRequest request = new CreateTestcaseRequest("new-input", "new-output", true);
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));
            when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));

            Testcase result = testcaseService.updateTestcase(1L, request, problemSetter);

            assertThat(result.getInput()).isEqualTo("new-input");
            assertThat(result.getExpectedOutput()).isEqualTo("new-output");
            assertThat(result.isHidden()).isTrue();
            verify(testcaseRepository).save(testcase);
        }

        @Test
        @DisplayName("should update testcase when admin updates any problem")
        void shouldUpdateTestcaseWhenAdminUpdatesAnyProblem() {
            CreateTestcaseRequest request = new CreateTestcaseRequest("new-input", "new-output", false);
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));
            when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));

            Testcase result = testcaseService.updateTestcase(1L, request, admin);

            assertThat(result.getInput()).isEqualTo("new-input");
            assertThat(result.getExpectedOutput()).isEqualTo("new-output");
            assertThat(result.isHidden()).isFalse();
            verify(testcaseRepository).save(testcase);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when other problem setter tries to update")
        void shouldThrowAccessDeniedWhenOtherProblemSetterTriesToUpdate() {
            CreateTestcaseRequest request = new CreateTestcaseRequest("new-input", "new-output", false);
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));

            assertThatThrownBy(() -> testcaseService.updateTestcase(1L, request, otherProblemSetter))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");

            verify(testcaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when regular user tries to update")
        void shouldThrowAccessDeniedWhenRegularUserTriesToUpdate() {
            CreateTestcaseRequest request = new CreateTestcaseRequest("new-input", "new-output", false);
            when(testcaseRepository.findById(1L)).thenReturn(Optional.of(testcase));

            assertThatThrownBy(() -> testcaseService.updateTestcase(1L, request, regularUser))
                .isInstanceOf(AccessDeniedException.class);

            verify(testcaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when testcase not found")
        void shouldThrowResourceNotFoundWhenTestcaseNotFound() {
            CreateTestcaseRequest request = new CreateTestcaseRequest("new-input", "new-output", false);
            when(testcaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testcaseService.updateTestcase(999L, request, problemSetter))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Testcase not found");
        }
    }
}
