package com.codearena.entity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Problem} entity.
 * Tests entity creation, relationships, and helper methods.
 */
@DisplayName("Problem Entity")
class ProblemTest {

    private User testUser;
    private Problem problem;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "hashedPassword", Role.PROBLEM_SETTER);
        testUser.setId(1L);
        
        problem = new Problem("Two Sum", "two-sum", "Find two numbers that add up to target", 
                Difficulty.EASY, testUser);
    }

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("should create problem with required fields")
        void shouldCreateProblemWithRequiredFields() {
            assertThat(problem.getTitle()).isEqualTo("Two Sum");
            assertThat(problem.getSlug()).isEqualTo("two-sum");
            assertThat(problem.getStatement()).isEqualTo("Find two numbers that add up to target");
            assertThat(problem.getDifficulty()).isEqualTo(Difficulty.EASY);
            assertThat(problem.getCreatedBy()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("should have default status as DRAFT")
        void shouldHaveDefaultStatusAsDraft() {
            assertThat(problem.getStatus()).isEqualTo(ProblemStatus.DRAFT);
        }

        @Test
        @DisplayName("should have empty tags list by default")
        void shouldHaveEmptyTagsListByDefault() {
            assertThat(problem.getTags()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should have empty testcases list by default")
        void shouldHaveEmptyTestcasesListByDefault() {
            assertThat(problem.getTestcases()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should have empty starterCodes list by default")
        void shouldHaveEmptyStarterCodesListByDefault() {
            assertThat(problem.getStarterCodes()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should have createdAt set to current time")
        void shouldHaveCreatedAtSetToCurrentTime() {
            LocalDateTime now = LocalDateTime.now();
            assertThat(problem.getCreatedAt()).isNotNull();
            assertThat(problem.getCreatedAt()).isBeforeOrEqualTo(now);
        }

        @Test
        @DisplayName("should have null updatedAt initially")
        void shouldHaveNullUpdatedAtInitially() {
            assertThat(problem.getUpdatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("should set and get title")
        void shouldSetAndGetTitle() {
            problem.setTitle("Three Sum");
            assertThat(problem.getTitle()).isEqualTo("Three Sum");
        }

        @Test
        @DisplayName("should set and get slug")
        void shouldSetAndGetSlug() {
            problem.setSlug("three-sum");
            assertThat(problem.getSlug()).isEqualTo("three-sum");
        }

        @Test
        @DisplayName("should set and get statement")
        void shouldSetAndGetStatement() {
            problem.setStatement("New statement");
            assertThat(problem.getStatement()).isEqualTo("New statement");
        }

        @Test
        @DisplayName("should set and get constraints")
        void shouldSetAndGetConstraints() {
            problem.setConstraints("1 <= n <= 1000");
            assertThat(problem.getConstraints()).isEqualTo("1 <= n <= 1000");
        }

        @Test
        @DisplayName("should set and get difficulty")
        void shouldSetAndGetDifficulty() {
            problem.setDifficulty(Difficulty.HARD);
            assertThat(problem.getDifficulty()).isEqualTo(Difficulty.HARD);
        }

        @Test
        @DisplayName("should set and get status")
        void shouldSetAndGetStatus() {
            problem.setStatus(ProblemStatus.PUBLISHED);
            assertThat(problem.getStatus()).isEqualTo(ProblemStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should set and get tags")
        void shouldSetAndGetTags() {
            List<String> tags = Arrays.asList("array", "hash-table");
            problem.setTags(tags);
            assertThat(problem.getTags()).containsExactly("array", "hash-table");
        }

        @Test
        @DisplayName("should handle null tags by setting empty list")
        void shouldHandleNullTagsBySettingEmptyList() {
            problem.setTags(null);
            assertThat(problem.getTags()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should set and get id")
        void shouldSetAndGetId() {
            problem.setId(100L);
            assertThat(problem.getId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("Testcase Relationship")
    class TestcaseRelationship {

        @Test
        @DisplayName("should add testcase and set bidirectional relationship")
        void shouldAddTestcaseAndSetBidirectionalRelationship() {
            Testcase testcase = new Testcase();
            testcase.setInput("[1,2,3]");
            testcase.setExpectedOutput("6");

            problem.addTestcase(testcase);

            assertThat(problem.getTestcases()).hasSize(1).contains(testcase);
            assertThat(testcase.getProblem()).isEqualTo(problem);
        }

        @Test
        @DisplayName("should remove testcase and clear bidirectional relationship")
        void shouldRemoveTestcaseAndClearBidirectionalRelationship() {
            Testcase testcase = new Testcase();
            testcase.setInput("[1,2,3]");
            testcase.setExpectedOutput("6");

            problem.addTestcase(testcase);
            problem.removeTestcase(testcase);

            assertThat(problem.getTestcases()).isEmpty();
            assertThat(testcase.getProblem()).isNull();
        }

        @Test
        @DisplayName("should add multiple testcases")
        void shouldAddMultipleTestcases() {
            Testcase testcase1 = new Testcase();
            testcase1.setInput("[1,2]");
            testcase1.setExpectedOutput("3");

            Testcase testcase2 = new Testcase();
            testcase2.setInput("[3,4]");
            testcase2.setExpectedOutput("7");

            problem.addTestcase(testcase1);
            problem.addTestcase(testcase2);

            assertThat(problem.getTestcases()).hasSize(2).contains(testcase1, testcase2);
        }
    }

    @Nested
    @DisplayName("StarterCode Relationship")
    class StarterCodeRelationship {

        @Test
        @DisplayName("should add starter code and set bidirectional relationship")
        void shouldAddStarterCodeAndSetBidirectionalRelationship() {
            StarterCode starterCode = new StarterCode();
            starterCode.setLanguageId(62); // Java
            starterCode.setCode("class Solution {}");

            problem.addStarterCode(starterCode);

            assertThat(problem.getStarterCodes()).hasSize(1).contains(starterCode);
            assertThat(starterCode.getProblem()).isEqualTo(problem);
        }

        @Test
        @DisplayName("should remove starter code and clear bidirectional relationship")
        void shouldRemoveStarterCodeAndClearBidirectionalRelationship() {
            StarterCode starterCode = new StarterCode();
            starterCode.setLanguageId(62);
            starterCode.setCode("class Solution {}");

            problem.addStarterCode(starterCode);
            problem.removeStarterCode(starterCode);

            assertThat(problem.getStarterCodes()).isEmpty();
            assertThat(starterCode.getProblem()).isNull();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            problem.setId(1L);
            assertThat(problem).isEqualTo(problem);
        }

        @Test
        @DisplayName("should be equal to another problem with same id")
        void shouldBeEqualToAnotherProblemWithSameId() {
            problem.setId(1L);
            
            Problem other = new Problem();
            other.setId(1L);
            
            assertThat(problem).isEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to problem with different id")
        void shouldNotBeEqualToProblemWithDifferentId() {
            problem.setId(1L);
            
            Problem other = new Problem();
            other.setId(2L);
            
            assertThat(problem).isNotEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            problem.setId(1L);
            assertThat(problem).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            problem.setId(1L);
            assertThat(problem).isNotEqualTo("not a problem");
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            problem.setId(1L);
            int hashCode1 = problem.hashCode();
            int hashCode2 = problem.hashCode();
            assertThat(hashCode1).isEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        @DisplayName("should include key fields in toString")
        void shouldIncludeKeyFieldsInToString() {
            problem.setId(1L);
            String result = problem.toString();
            
            assertThat(result).contains("id=1");
            assertThat(result).contains("title='Two Sum'");
            assertThat(result).contains("slug='two-sum'");
            assertThat(result).contains("difficulty=EASY");
            assertThat(result).contains("status=DRAFT");
        }
    }
}
