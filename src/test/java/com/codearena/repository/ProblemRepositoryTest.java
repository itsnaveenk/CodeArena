package com.codearena.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.User;

/**
 * Repository tests for {@link ProblemRepository}.
 * Tests custom queries and filtering functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProblemRepository")
class ProblemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProblemRepository problemRepository;

    private User testUser;
    private Problem publishedEasyProblem;
    private Problem publishedMediumProblem;
    private Problem draftProblem;
    private Problem pendingProblem;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User("Test User", "test@example.com", "hashedPassword", Role.PROBLEM_SETTER);
        entityManager.persist(testUser);

        // Create published easy problem
        publishedEasyProblem = new Problem("Two Sum", "two-sum", 
                "Find two numbers that add up to target", Difficulty.EASY, testUser);
        publishedEasyProblem.setStatus(ProblemStatus.PUBLISHED);
        publishedEasyProblem.setTags(Arrays.asList("array", "hash-table"));
        entityManager.persist(publishedEasyProblem);

        // Create published medium problem
        publishedMediumProblem = new Problem("Three Sum", "three-sum", 
                "Find three numbers that add up to zero", Difficulty.MEDIUM, testUser);
        publishedMediumProblem.setStatus(ProblemStatus.PUBLISHED);
        publishedMediumProblem.setTags(Arrays.asList("array", "two-pointers"));
        entityManager.persist(publishedMediumProblem);

        // Create draft problem
        draftProblem = new Problem("Four Sum", "four-sum", 
                "Find four numbers that add up to target", Difficulty.HARD, testUser);
        draftProblem.setStatus(ProblemStatus.DRAFT);
        entityManager.persist(draftProblem);

        // Create pending review problem
        pendingProblem = new Problem("Five Sum", "five-sum", 
                "Find five numbers", Difficulty.HARD, testUser);
        pendingProblem.setStatus(ProblemStatus.PENDING_REVIEW);
        entityManager.persist(pendingProblem);

        entityManager.flush();
    }

    @Nested
    @DisplayName("findBySlug")
    class FindBySlug {

        @Test
        @DisplayName("should find problem by slug")
        void shouldFindProblemBySlug() {
            Optional<Problem> result = problemRepository.findBySlug("two-sum");
            
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Two Sum");
        }

        @Test
        @DisplayName("should return empty for non-existent slug")
        void shouldReturnEmptyForNonExistentSlug() {
            Optional<Problem> result = problemRepository.findBySlug("non-existent");
            
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsBySlug")
    class ExistsBySlug {

        @Test
        @DisplayName("should return true for existing slug")
        void shouldReturnTrueForExistingSlug() {
            boolean exists = problemRepository.existsBySlug("two-sum");
            
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent slug")
        void shouldReturnFalseForNonExistentSlug() {
            boolean exists = problemRepository.existsBySlug("non-existent");
            
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should find all published problems")
        void shouldFindAllPublishedProblems() {
            Page<Problem> result = problemRepository.findByStatus(ProblemStatus.PUBLISHED, PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).extracting(Problem::getTitle)
                    .containsExactlyInAnyOrder("Two Sum", "Three Sum");
        }

        @Test
        @DisplayName("should find all draft problems")
        void shouldFindAllDraftProblems() {
            Page<Problem> result = problemRepository.findByStatus(ProblemStatus.DRAFT, PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Four Sum");
        }
    }

    @Nested
    @DisplayName("findAllPublished")
    class FindAllPublished {

        @Test
        @DisplayName("should return only published problems")
        void shouldReturnOnlyPublishedProblems() {
            Page<Problem> result = problemRepository.findAllPublished(PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getStatus() == ProblemStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should support pagination")
        void shouldSupportPagination() {
            Page<Problem> page1 = problemRepository.findAllPublished(PageRequest.of(0, 1));
            Page<Problem> page2 = problemRepository.findAllPublished(PageRequest.of(1, 1));
            
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page2.getContent()).hasSize(1);
            assertThat(page1.getTotalElements()).isEqualTo(2);
            assertThat(page1.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findPublishedByDifficulty")
    class FindPublishedByDifficulty {

        @Test
        @DisplayName("should find published problems by difficulty")
        void shouldFindPublishedProblemsByDifficulty() {
            Page<Problem> result = problemRepository.findPublishedByDifficulty(Difficulty.EASY, PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Two Sum");
        }

        @Test
        @DisplayName("should not include non-published problems")
        void shouldNotIncludeNonPublishedProblems() {
            Page<Problem> result = problemRepository.findPublishedByDifficulty(Difficulty.HARD, PageRequest.of(0, 10));
            
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPublishedByTitleContaining")
    class FindPublishedByTitleContaining {

        @Test
        @DisplayName("should find problems by title keyword (case-insensitive)")
        void shouldFindProblemsByTitleKeywordCaseInsensitive() {
            Page<Problem> result = problemRepository.findPublishedByTitleContaining("sum", PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("should find problems with partial match")
        void shouldFindProblemsWithPartialMatch() {
            Page<Problem> result = problemRepository.findPublishedByTitleContaining("Two", PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Two Sum");
        }

        @Test
        @DisplayName("should not include non-published problems in search")
        void shouldNotIncludeNonPublishedProblemsInSearch() {
            Page<Problem> result = problemRepository.findPublishedByTitleContaining("Four", PageRequest.of(0, 10));
            
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPublishedByDifficultyAndTitleContaining")
    class FindPublishedByDifficultyAndTitleContaining {

        @Test
        @DisplayName("should filter by both difficulty and title")
        void shouldFilterByBothDifficultyAndTitle() {
            Page<Problem> result = problemRepository.findPublishedByDifficultyAndTitleContaining(
                    Difficulty.EASY, "Sum", PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Two Sum");
        }

        @Test
        @DisplayName("should return empty when no match")
        void shouldReturnEmptyWhenNoMatch() {
            Page<Problem> result = problemRepository.findPublishedByDifficultyAndTitleContaining(
                    Difficulty.HARD, "Sum", PageRequest.of(0, 10));
            
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCreatedById")
    class FindByCreatedById {

        @Test
        @DisplayName("should find all problems by creator")
        void shouldFindAllProblemsByCreator() {
            Page<Problem> result = problemRepository.findByCreatedById(testUser.getId(), PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(4);
        }

        @Test
        @DisplayName("should return empty for non-existent creator")
        void shouldReturnEmptyForNonExistentCreator() {
            Page<Problem> result = problemRepository.findByCreatedById(999L, PageRequest.of(0, 10));
            
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllPendingReview")
    class FindAllPendingReview {

        @Test
        @DisplayName("should find all pending review problems")
        void shouldFindAllPendingReviewProblems() {
            Page<Problem> result = problemRepository.findAllPendingReview(PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Five Sum");
        }
    }

    @Nested
    @DisplayName("countByStatus")
    class CountByStatus {

        @Test
        @DisplayName("should count problems by status")
        void shouldCountProblemsByStatus() {
            long publishedCount = problemRepository.countByStatus(ProblemStatus.PUBLISHED);
            long draftCount = problemRepository.countByStatus(ProblemStatus.DRAFT);
            
            assertThat(publishedCount).isEqualTo(2);
            assertThat(draftCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countPublishedByDifficulty")
    class CountPublishedByDifficulty {

        @Test
        @DisplayName("should count published problems by difficulty")
        void shouldCountPublishedProblemsByDifficulty() {
            long easyCount = problemRepository.countPublishedByDifficulty(Difficulty.EASY);
            long mediumCount = problemRepository.countPublishedByDifficulty(Difficulty.MEDIUM);
            long hardCount = problemRepository.countPublishedByDifficulty(Difficulty.HARD);
            
            assertThat(easyCount).isEqualTo(1);
            assertThat(mediumCount).isEqualTo(1);
            assertThat(hardCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findByIdIn")
    class FindByIdIn {

        @Test
        @DisplayName("should find problems by list of ids")
        void shouldFindProblemsByListOfIds() {
            List<Long> ids = Arrays.asList(publishedEasyProblem.getId(), publishedMediumProblem.getId());
            List<Problem> result = problemRepository.findByIdIn(ids);
            
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty for empty id list")
        void shouldReturnEmptyForEmptyIdList() {
            List<Problem> result = problemRepository.findByIdIn(List.of());
            
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatusAndDifficulty")
    class FindByStatusAndDifficulty {

        @Test
        @DisplayName("should find problems by status and difficulty")
        void shouldFindProblemsByStatusAndDifficulty() {
            Page<Problem> result = problemRepository.findByStatusAndDifficulty(
                    ProblemStatus.PUBLISHED, Difficulty.EASY, PageRequest.of(0, 10));
            
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Two Sum");
        }
    }
}
