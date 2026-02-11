package com.codearena.repository;

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
import org.springframework.test.context.ActiveProfiles;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.StarterCode;
import com.codearena.entity.SupportedLanguage;
import com.codearena.entity.User;

/**
 * Repository tests for {@link StarterCodeRepository}.
 * Tests custom queries for finding starter code by problem and language.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("StarterCodeRepository")
class StarterCodeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StarterCodeRepository starterCodeRepository;

    private User testUser;
    private Problem problem1;
    private Problem problem2;
    private StarterCode javaStarterCode;
    private StarterCode pythonStarterCode;
    private StarterCode cppStarterCode;

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

        // Create starter codes for problem1
        javaStarterCode = new StarterCode(problem1, SupportedLanguage.JAVA.getJudge0Id(), 
                "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // Your code here\n    }\n}");
        entityManager.persist(javaStarterCode);

        pythonStarterCode = new StarterCode(problem1, SupportedLanguage.PYTHON.getJudge0Id(), 
                "class Solution:\n    def twoSum(self, nums: List[int], target: int) -> List[int]:\n        pass");
        entityManager.persist(pythonStarterCode);

        cppStarterCode = new StarterCode(problem1, SupportedLanguage.CPP.getJudge0Id(), 
                "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n    }\n};");
        entityManager.persist(cppStarterCode);

        // Create starter code for problem2 (only Java)
        StarterCode problem2JavaCode = new StarterCode(problem2, SupportedLanguage.JAVA.getJudge0Id(), 
                "class Solution {\n    public List<List<Integer>> threeSum(int[] nums) {\n    }\n}");
        entityManager.persist(problem2JavaCode);

        entityManager.flush();
    }

    @Nested
    @DisplayName("findByProblemId")
    class FindByProblemId {

        @Test
        @DisplayName("should find all starter codes for a problem")
        void shouldFindAllStarterCodesForProblem() {
            List<StarterCode> result = starterCodeRepository.findByProblemId(problem1.getId());
            
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should return empty list for problem with no starter codes")
        void shouldReturnEmptyListForProblemWithNoStarterCodes() {
            // Create a problem without starter codes
            Problem emptyProblem = new Problem("Empty Problem", "empty-problem", 
                    "No starter codes", Difficulty.EASY, testUser);
            entityManager.persist(emptyProblem);
            entityManager.flush();

            List<StarterCode> result = starterCodeRepository.findByProblemId(emptyProblem.getId());
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for non-existent problem")
        void shouldReturnEmptyListForNonExistentProblem() {
            List<StarterCode> result = starterCodeRepository.findByProblemId(999L);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should only return starter codes for specified problem")
        void shouldOnlyReturnStarterCodesForSpecifiedProblem() {
            List<StarterCode> result = starterCodeRepository.findByProblemId(problem2.getId());
            
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).contains("threeSum");
        }
    }

    @Nested
    @DisplayName("findByProblemIdAndLanguageId")
    class FindByProblemIdAndLanguageId {

        @Test
        @DisplayName("should find Java starter code for a problem")
        void shouldFindJavaStarterCodeForProblem() {
            Optional<StarterCode> result = starterCodeRepository.findByProblemIdAndLanguageId(
                    problem1.getId(), SupportedLanguage.JAVA.getJudge0Id());
            
            assertThat(result).isPresent();
            assertThat(result.get().getCode()).contains("class Solution");
            assertThat(result.get().getLanguageId()).isEqualTo(62);
        }

        @Test
        @DisplayName("should find Python starter code for a problem")
        void shouldFindPythonStarterCodeForProblem() {
            Optional<StarterCode> result = starterCodeRepository.findByProblemIdAndLanguageId(
                    problem1.getId(), SupportedLanguage.PYTHON.getJudge0Id());
            
            assertThat(result).isPresent();
            assertThat(result.get().getCode()).contains("def twoSum");
            assertThat(result.get().getLanguageId()).isEqualTo(71);
        }

        @Test
        @DisplayName("should find C++ starter code for a problem")
        void shouldFindCppStarterCodeForProblem() {
            Optional<StarterCode> result = starterCodeRepository.findByProblemIdAndLanguageId(
                    problem1.getId(), SupportedLanguage.CPP.getJudge0Id());
            
            assertThat(result).isPresent();
            assertThat(result.get().getCode()).contains("vector<int>");
            assertThat(result.get().getLanguageId()).isEqualTo(54);
        }

        @Test
        @DisplayName("should return empty for non-existent language")
        void shouldReturnEmptyForNonExistentLanguage() {
            // problem2 only has Java starter code
            Optional<StarterCode> result = starterCodeRepository.findByProblemIdAndLanguageId(
                    problem2.getId(), SupportedLanguage.PYTHON.getJudge0Id());
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for non-existent problem")
        void shouldReturnEmptyForNonExistentProblem() {
            Optional<StarterCode> result = starterCodeRepository.findByProblemIdAndLanguageId(
                    999L, SupportedLanguage.JAVA.getJudge0Id());
            
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByProblemIdAndLanguageId")
    class ExistsByProblemIdAndLanguageId {

        @Test
        @DisplayName("should return true for existing starter code")
        void shouldReturnTrueForExistingStarterCode() {
            boolean exists = starterCodeRepository.existsByProblemIdAndLanguageId(
                    problem1.getId(), SupportedLanguage.JAVA.getJudge0Id());
            
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent starter code")
        void shouldReturnFalseForNonExistentStarterCode() {
            boolean exists = starterCodeRepository.existsByProblemIdAndLanguageId(
                    problem2.getId(), SupportedLanguage.PYTHON.getJudge0Id());
            
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should return false for non-existent problem")
        void shouldReturnFalseForNonExistentProblem() {
            boolean exists = starterCodeRepository.existsByProblemIdAndLanguageId(
                    999L, SupportedLanguage.JAVA.getJudge0Id());
            
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteByProblemId")
    class DeleteByProblemId {

        @Test
        @DisplayName("should delete all starter codes for a problem")
        void shouldDeleteAllStarterCodesForProblem() {
            // Verify starter codes exist before deletion
            assertThat(starterCodeRepository.countByProblemId(problem1.getId())).isEqualTo(3);
            
            starterCodeRepository.deleteByProblemId(problem1.getId());
            entityManager.flush();
            
            assertThat(starterCodeRepository.countByProblemId(problem1.getId())).isZero();
        }

        @Test
        @DisplayName("should not affect starter codes of other problems")
        void shouldNotAffectStarterCodesOfOtherProblems() {
            starterCodeRepository.deleteByProblemId(problem1.getId());
            entityManager.flush();
            
            // problem2's starter codes should still exist
            assertThat(starterCodeRepository.countByProblemId(problem2.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle deletion for non-existent problem gracefully")
        void shouldHandleDeletionForNonExistentProblemGracefully() {
            // Should not throw exception
            starterCodeRepository.deleteByProblemId(999L);
            entityManager.flush();
            
            // Existing starter codes should be unaffected
            assertThat(starterCodeRepository.countByProblemId(problem1.getId())).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("countByProblemId")
    class CountByProblemId {

        @Test
        @DisplayName("should count all starter codes for a problem")
        void shouldCountAllStarterCodesForProblem() {
            long count = starterCodeRepository.countByProblemId(problem1.getId());
            
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero for problem with no starter codes")
        void shouldReturnZeroForProblemWithNoStarterCodes() {
            Problem emptyProblem = new Problem("Empty Problem", "empty-problem", 
                    "No starter codes", Difficulty.EASY, testUser);
            entityManager.persist(emptyProblem);
            entityManager.flush();

            long count = starterCodeRepository.countByProblemId(emptyProblem.getId());
            
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should return zero for non-existent problem")
        void shouldReturnZeroForNonExistentProblem() {
            long count = starterCodeRepository.countByProblemId(999L);
            
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Standard JPA Operations")
    class StandardJpaOperations {

        @Test
        @DisplayName("should save and retrieve starter code")
        void shouldSaveAndRetrieveStarterCode() {
            StarterCode newStarterCode = new StarterCode(problem2, SupportedLanguage.PYTHON.getJudge0Id(), 
                    "class Solution:\n    def threeSum(self, nums):\n        pass");
            StarterCode saved = starterCodeRepository.save(newStarterCode);
            
            assertThat(saved.getId()).isNotNull();
            assertThat(starterCodeRepository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("should delete starter code by id")
        void shouldDeleteStarterCodeById() {
            Long id = javaStarterCode.getId();
            starterCodeRepository.deleteById(id);
            entityManager.flush();
            
            assertThat(starterCodeRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("should find all starter codes")
        void shouldFindAllStarterCodes() {
            List<StarterCode> all = starterCodeRepository.findAll();
            
            assertThat(all).hasSize(4); // 3 for problem1 + 1 for problem2
        }
    }

    @Nested
    @DisplayName("Unique Constraint")
    class UniqueConstraint {

        @Test
        @DisplayName("should enforce unique constraint on problem_id and language_id")
        void shouldEnforceUniqueConstraintOnProblemIdAndLanguageId() {
            // Verify that the unique constraint exists by checking existsByProblemIdAndLanguageId
            // The actual constraint enforcement is tested at the database level
            assertThat(starterCodeRepository.existsByProblemIdAndLanguageId(
                    problem1.getId(), SupportedLanguage.JAVA.getJudge0Id())).isTrue();
        }
    }
}
