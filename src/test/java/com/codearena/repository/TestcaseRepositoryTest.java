package com.codearena.repository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;

/**
 * Repository tests for {@link TestcaseRepository}.
 * Tests custom queries for finding testcases by problem and visibility.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TestcaseRepository")
class TestcaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestcaseRepository testcaseRepository;

    private User testUser;
    private Problem problem1;
    private Problem problem2;
    private Testcase visibleTestcase1;
    private Testcase visibleTestcase2;
    private Testcase hiddenTestcase1;
    private Testcase hiddenTestcase2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User("Test User", "test@example.com", "hashedPassword", Role.PROBLEM_SETTER);
        entityManager.persist(testUser);

        // Create first problem
        problem1 = new Problem("Two Sum", "two-sum", 
                "Find two numbers that add up to target", Difficulty.EASY, testUser);
        entityManager.persist(problem1);

        // Create second problem
        problem2 = new Problem("Three Sum", "three-sum", 
                "Find three numbers that add up to zero", Difficulty.MEDIUM, testUser);
        entityManager.persist(problem2);

        // Create visible testcases for problem1
        visibleTestcase1 = new Testcase(problem1, "[2,7,11,15]\n9", "[0,1]", false);
        entityManager.persist(visibleTestcase1);

        visibleTestcase2 = new Testcase(problem1, "[3,2,4]\n6", "[1,2]", false);
        entityManager.persist(visibleTestcase2);

        // Create hidden testcases for problem1
        hiddenTestcase1 = new Testcase(problem1, "[1,2,3,4,5]\n9", "[3,4]", true);
        entityManager.persist(hiddenTestcase1);

        hiddenTestcase2 = new Testcase(problem1, "[0,0,0,0]\n0", "[0,1]", true);
        entityManager.persist(hiddenTestcase2);

        // Create testcase for problem2
        Testcase problem2Testcase = new Testcase(problem2, "[-1,0,1,2,-1,-4]", "[[-1,-1,2],[-1,0,1]]", false);
        entityManager.persist(problem2Testcase);

        entityManager.flush();
    }

    @Nested
    @DisplayName("findByProblemId")
    class FindByProblemId {

        @Test
        @DisplayName("should find all testcases for a problem")
        void shouldFindAllTestcasesForProblem() {
            List<Testcase> result = testcaseRepository.findByProblemId(problem1.getId());
            
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("should return empty list for problem with no testcases")
        void shouldReturnEmptyListForProblemWithNoTestcases() {
            // Create a problem without testcases
            Problem emptyProblem = new Problem("Empty Problem", "empty-problem", 
                    "No testcases", Difficulty.EASY, testUser);
            entityManager.persist(emptyProblem);
            entityManager.flush();

            List<Testcase> result = testcaseRepository.findByProblemId(emptyProblem.getId());
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for non-existent problem")
        void shouldReturnEmptyListForNonExistentProblem() {
            List<Testcase> result = testcaseRepository.findByProblemId(999L);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should only return testcases for specified problem")
        void shouldOnlyReturnTestcasesForSpecifiedProblem() {
            List<Testcase> result = testcaseRepository.findByProblemId(problem2.getId());
            
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInput()).contains("-1,0,1");
        }
    }

    @Nested
    @DisplayName("findByProblemIdAndIsHidden")
    class FindByProblemIdAndIsHidden {

        @Test
        @DisplayName("should find visible testcases for a problem")
        void shouldFindVisibleTestcasesForProblem() {
            List<Testcase> result = testcaseRepository.findByProblemIdAndIsHidden(problem1.getId(), false);
            
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(tc -> !tc.isHidden());
        }

        @Test
        @DisplayName("should find hidden testcases for a problem")
        void shouldFindHiddenTestcasesForProblem() {
            List<Testcase> result = testcaseRepository.findByProblemIdAndIsHidden(problem1.getId(), true);
            
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Testcase::isHidden);
        }

        @Test
        @DisplayName("should return empty list when no matching testcases")
        void shouldReturnEmptyListWhenNoMatchingTestcases() {
            // problem2 has no hidden testcases
            List<Testcase> result = testcaseRepository.findByProblemIdAndIsHidden(problem2.getId(), true);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for non-existent problem")
        void shouldReturnEmptyListForNonExistentProblem() {
            List<Testcase> result = testcaseRepository.findByProblemIdAndIsHidden(999L, false);
            
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByProblemId")
    class CountByProblemId {

        @Test
        @DisplayName("should count all testcases for a problem")
        void shouldCountAllTestcasesForProblem() {
            long count = testcaseRepository.countByProblemId(problem1.getId());
            
            assertThat(count).isEqualTo(4);
        }

        @Test
        @DisplayName("should return zero for problem with no testcases")
        void shouldReturnZeroForProblemWithNoTestcases() {
            Problem emptyProblem = new Problem("Empty Problem", "empty-problem", 
                    "No testcases", Difficulty.EASY, testUser);
            entityManager.persist(emptyProblem);
            entityManager.flush();

            long count = testcaseRepository.countByProblemId(emptyProblem.getId());
            
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should return zero for non-existent problem")
        void shouldReturnZeroForNonExistentProblem() {
            long count = testcaseRepository.countByProblemId(999L);
            
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("countByProblemIdAndIsHidden")
    class CountByProblemIdAndIsHidden {

        @Test
        @DisplayName("should count visible testcases for a problem")
        void shouldCountVisibleTestcasesForProblem() {
            long count = testcaseRepository.countByProblemIdAndIsHidden(problem1.getId(), false);
            
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should count hidden testcases for a problem")
        void shouldCountHiddenTestcasesForProblem() {
            long count = testcaseRepository.countByProblemIdAndIsHidden(problem1.getId(), true);
            
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero when no matching testcases")
        void shouldReturnZeroWhenNoMatchingTestcases() {
            long count = testcaseRepository.countByProblemIdAndIsHidden(problem2.getId(), true);
            
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("deleteByProblemId")
    class DeleteByProblemId {

        @Test
        @DisplayName("should delete all testcases for a problem")
        void shouldDeleteAllTestcasesForProblem() {
            // Verify testcases exist before deletion
            assertThat(testcaseRepository.countByProblemId(problem1.getId())).isEqualTo(4);
            
            testcaseRepository.deleteByProblemId(problem1.getId());
            entityManager.flush();
            
            assertThat(testcaseRepository.countByProblemId(problem1.getId())).isZero();
        }

        @Test
        @DisplayName("should not affect testcases of other problems")
        void shouldNotAffectTestcasesOfOtherProblems() {
            testcaseRepository.deleteByProblemId(problem1.getId());
            entityManager.flush();
            
            // problem2's testcases should still exist
            assertThat(testcaseRepository.countByProblemId(problem2.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle deletion for non-existent problem gracefully")
        void shouldHandleDeletionForNonExistentProblemGracefully() {
            // Should not throw exception
            testcaseRepository.deleteByProblemId(999L);
            entityManager.flush();
            
            // Existing testcases should be unaffected
            assertThat(testcaseRepository.countByProblemId(problem1.getId())).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Standard JPA Operations")
    class StandardJpaOperations {

        @Test
        @DisplayName("should save and retrieve testcase")
        void shouldSaveAndRetrieveTestcase() {
            Testcase newTestcase = new Testcase(problem1, "[10,20,30]", "60", false);
            Testcase saved = testcaseRepository.save(newTestcase);
            
            assertThat(saved.getId()).isNotNull();
            assertThat(testcaseRepository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("should delete testcase by id")
        void shouldDeleteTestcaseById() {
            Long id = visibleTestcase1.getId();
            testcaseRepository.deleteById(id);
            entityManager.flush();
            
            assertThat(testcaseRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("should find all testcases")
        void shouldFindAllTestcases() {
            List<Testcase> all = testcaseRepository.findAll();
            
            assertThat(all).hasSize(5); // 4 for problem1 + 1 for problem2
        }
    }
}
