package com.codearena.service;

import java.time.LocalDateTime;
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

import com.codearena.dto.ContestEditorialDto;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestEditorial;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.TimerMode;
import com.codearena.entity.User;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.ContestValidationException;
import com.codearena.repository.ContestEditorialRepository;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;

/**
 * Unit tests for ContestEditorialService.
 * 
 * Requirements covered:
 * - 9.3.1: Allow Contest_Manager to add/edit editorial content for each contest problem
 * - 9.3.2: Editorial fields: problem_id, content (markdown, max 10000 chars), created_at, updated_at
 * - 9.3.3: When contest.status=FINISHED AND editorial exists, display to ALL users
 * - 9.3.4: Allow adding editorials only after contest is FINISHED
 */
@ExtendWith(MockitoExtension.class)
class ContestEditorialServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ContestProblemRepository contestProblemRepository;

    @Mock
    private ContestEditorialRepository contestEditorialRepository;

    private ContestEditorialService contestEditorialService;

    @BeforeEach
    void setUp() {
        contestEditorialService = new ContestEditorialServiceImpl(
            contestRepository, contestProblemRepository, contestEditorialRepository);
    }

    private User createUser(Long id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Contest createContest(Long id, String title, ContestStatus status) {
        User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
        Contest contest = new Contest(title, title.toLowerCase().replace(" ", "-"),
            "Description for " + title, TimerMode.GLOBAL,
            LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), creator);
        contest.setId(id);
        contest.setStatus(status);
        return contest;
    }

    private Problem createProblem(Long id, String title) {
        User creator = createUser(1L, "Creator", "creator@example.com", Role.PROBLEM_SETTER);
        Problem problem = new Problem(title, title.toLowerCase().replace(" ", "-"),
            "Statement for " + title, Difficulty.MEDIUM, creator);
        problem.setId(id);
        problem.setStatus(ProblemStatus.PUBLISHED);
        return problem;
    }

    private ContestProblem createContestProblem(Contest contest, Problem problem, int pointValue) {
        ContestProblem cp = new ContestProblem();
        cp.setId(1L);
        cp.setContest(contest);
        cp.setProblem(problem);
        cp.setPointValue(pointValue);
        cp.setDisplayOrder(1);
        return cp;
    }

    private ContestEditorial createEditorial(Contest contest, Problem problem, String content) {
        ContestEditorial editorial = new ContestEditorial(contest, problem, content);
        editorial.setId(1L);
        return editorial;
    }

    @Nested
    @DisplayName("createOrUpdateEditorial")
    class CreateOrUpdateEditorialTests {

        @Test
        @DisplayName("should create new editorial for finished contest")
        void shouldCreateNewEditorialForFinishedContest() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);
            String content = "This is the editorial content explaining the solution.";

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));
            when(contestEditorialRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.empty());
            when(contestEditorialRepository.save(any(ContestEditorial.class)))
                .thenAnswer(invocation -> {
                    ContestEditorial e = invocation.getArgument(0);
                    e.setId(1L);
                    return e;
                });

            // When
            ContestEditorial result = contestEditorialService.createOrUpdateEditorial(
                1L, 1L, content, author);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getContest()).isEqualTo(contest);
            assertThat(result.getProblem()).isEqualTo(problem);
            verify(contestEditorialRepository).save(any(ContestEditorial.class));
        }

        @Test
        @DisplayName("should update existing editorial")
        void shouldUpdateExistingEditorial() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);
            ContestEditorial existingEditorial = createEditorial(contest, problem, "Old content");
            String newContent = "Updated editorial content with better explanation.";

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));
            when(contestEditorialRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(existingEditorial));
            when(contestEditorialRepository.save(any(ContestEditorial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ContestEditorial result = contestEditorialService.createOrUpdateEditorial(
                1L, 1L, newContent, author);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(newContent);
            assertThat(result.getId()).isEqualTo(existingEditorial.getId());
            verify(contestEditorialRepository).save(existingEditorial);
        }

        @Test
        @DisplayName("should throw ContestNotFoundException when contest not found")
        void shouldThrowContestNotFoundExceptionWhenContestNotFound() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            when(contestRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                999L, 1L, "Content", author))
                .isInstanceOf(ContestNotFoundException.class);
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when contest is not FINISHED - DRAFT")
        void shouldThrowContestValidationExceptionWhenContestIsDraft() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.DRAFT);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, "Content", author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("FINISHED")
                .hasMessageContaining("DRAFT");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when contest is RUNNING")
        void shouldThrowContestValidationExceptionWhenContestIsRunning() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.RUNNING);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, "Content", author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("FINISHED")
                .hasMessageContaining("RUNNING");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when contest is PUBLISHED")
        void shouldThrowContestValidationExceptionWhenContestIsPublished() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.PUBLISHED);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, "Content", author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("FINISHED");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when problem not in contest")
        void shouldThrowContestValidationExceptionWhenProblemNotInContest() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 999L))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 999L, "Content", author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("not part of contest");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when content is empty")
        void shouldThrowContestValidationExceptionWhenContentIsEmpty() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, "", author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("cannot be empty");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when content is blank")
        void shouldThrowContestValidationExceptionWhenContentIsBlank() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, "   ", author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("cannot be empty");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when content is null")
        void shouldThrowContestValidationExceptionWhenContentIsNull() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, null, author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("cannot be empty");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ContestValidationException when content exceeds max length")
        void shouldThrowContestValidationExceptionWhenContentExceedsMaxLength() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);
            String longContent = "x".repeat(10001); // Exceeds 10000 char limit

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.createOrUpdateEditorial(
                1L, 1L, longContent, author))
                .isInstanceOf(ContestValidationException.class)
                .hasMessageContaining("exceeds maximum length")
                .hasMessageContaining("10000");
            
            verify(contestEditorialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should accept content at exactly max length")
        void shouldAcceptContentAtExactlyMaxLength() {
            // Given
            User author = createUser(1L, "Author", "author@example.com", Role.PROBLEM_SETTER);
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestProblem contestProblem = createContestProblem(contest, problem, 100);
            String maxContent = "x".repeat(10000); // Exactly at limit

            when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
            when(contestProblemRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(contestProblem));
            when(contestEditorialRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.empty());
            when(contestEditorialRepository.save(any(ContestEditorial.class)))
                .thenAnswer(invocation -> {
                    ContestEditorial e = invocation.getArgument(0);
                    e.setId(1L);
                    return e;
                });

            // When
            ContestEditorial result = contestEditorialService.createOrUpdateEditorial(
                1L, 1L, maxContent, author);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(10000);
            verify(contestEditorialRepository).save(any(ContestEditorial.class));
        }
    }

    @Nested
    @DisplayName("getEditorials")
    class GetEditorialsTests {

        @Test
        @DisplayName("should return all editorials for contest")
        void shouldReturnAllEditorialsForContest() {
            // Given
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem1 = createProblem(1L, "Problem 1");
            Problem problem2 = createProblem(2L, "Problem 2");
            ContestEditorial editorial1 = createEditorial(contest, problem1, "Editorial 1");
            ContestEditorial editorial2 = createEditorial(contest, problem2, "Editorial 2");
            editorial2.setId(2L);

            when(contestRepository.existsById(1L)).thenReturn(true);
            when(contestEditorialRepository.findByContestId(1L))
                .thenReturn(List.of(editorial1, editorial2));

            // When
            List<ContestEditorialDto> result = contestEditorialService.getEditorials(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).content()).isEqualTo("Editorial 1");
            assertThat(result.get(1).content()).isEqualTo("Editorial 2");
        }

        @Test
        @DisplayName("should return empty list when no editorials exist")
        void shouldReturnEmptyListWhenNoEditorialsExist() {
            // Given
            when(contestRepository.existsById(1L)).thenReturn(true);
            when(contestEditorialRepository.findByContestId(1L)).thenReturn(List.of());

            // When
            List<ContestEditorialDto> result = contestEditorialService.getEditorials(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ContestNotFoundException when contest not found")
        void shouldThrowContestNotFoundExceptionWhenContestNotFound() {
            // Given
            when(contestRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.getEditorials(999L))
                .isInstanceOf(ContestNotFoundException.class);
        }

        @Test
        @DisplayName("should include problem title in DTO")
        void shouldIncludeProblemTitleInDto() {
            // Given
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Two Sum");
            ContestEditorial editorial = createEditorial(contest, problem, "Editorial content");

            when(contestRepository.existsById(1L)).thenReturn(true);
            when(contestEditorialRepository.findByContestId(1L)).thenReturn(List.of(editorial));

            // When
            List<ContestEditorialDto> result = contestEditorialService.getEditorials(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).problemTitle()).isEqualTo("Two Sum");
            assertThat(result.get(0).problemId()).isEqualTo(1L);
            assertThat(result.get(0).contestId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getEditorial")
    class GetEditorialTests {

        @Test
        @DisplayName("should return editorial for specific problem")
        void shouldReturnEditorialForSpecificProblem() {
            // Given
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestEditorial editorial = createEditorial(contest, problem, "Editorial content");

            when(contestRepository.existsById(1L)).thenReturn(true);
            when(contestEditorialRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(editorial));

            // When
            ContestEditorialDto result = contestEditorialService.getEditorial(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("Editorial content");
            assertThat(result.problemId()).isEqualTo(1L);
            assertThat(result.contestId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return null when editorial not found")
        void shouldReturnNullWhenEditorialNotFound() {
            // Given
            when(contestRepository.existsById(1L)).thenReturn(true);
            when(contestEditorialRepository.findByContestIdAndProblemId(1L, 999L))
                .thenReturn(Optional.empty());

            // When
            ContestEditorialDto result = contestEditorialService.getEditorial(1L, 999L);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw ContestNotFoundException when contest not found")
        void shouldThrowContestNotFoundExceptionWhenContestNotFound() {
            // Given
            when(contestRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> contestEditorialService.getEditorial(999L, 1L))
                .isInstanceOf(ContestNotFoundException.class);
        }

        @Test
        @DisplayName("should include timestamps in DTO")
        void shouldIncludeTimestampsInDto() {
            // Given
            Contest contest = createContest(1L, "Test Contest", ContestStatus.FINISHED);
            Problem problem = createProblem(1L, "Test Problem");
            ContestEditorial editorial = createEditorial(contest, problem, "Editorial content");
            LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
            LocalDateTime updatedAt = LocalDateTime.now();
            editorial.setCreatedAt(createdAt);
            editorial.setUpdatedAt(updatedAt);

            when(contestRepository.existsById(1L)).thenReturn(true);
            when(contestEditorialRepository.findByContestIdAndProblemId(1L, 1L))
                .thenReturn(Optional.of(editorial));

            // When
            ContestEditorialDto result = contestEditorialService.getEditorial(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.createdAt()).isEqualTo(createdAt);
            assertThat(result.updatedAt()).isEqualTo(updatedAt);
        }
    }
}
