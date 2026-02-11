package com.codearena.entity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Testcase} entity.
 * Tests entity creation, properties, and relationships.
 */
@DisplayName("Testcase Entity")
class TestcaseTest {

    private User testUser;
    private Problem problem;
    private Testcase testcase;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "hashedPassword", Role.PROBLEM_SETTER);
        testUser.setId(1L);
        
        problem = new Problem("Two Sum", "two-sum", "Find two numbers that add up to target", 
                Difficulty.EASY, testUser);
        problem.setId(1L);
        
        testcase = new Testcase(problem, "[1,2,3]\n9", "6", false);
    }

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("should create testcase with all fields via constructor")
        void shouldCreateTestcaseWithAllFieldsViaConstructor() {
            assertThat(testcase.getProblem()).isEqualTo(problem);
            assertThat(testcase.getInput()).isEqualTo("[1,2,3]\n9");
            assertThat(testcase.getExpectedOutput()).isEqualTo("6");
            assertThat(testcase.isHidden()).isFalse();
        }

        @Test
        @DisplayName("should create hidden testcase")
        void shouldCreateHiddenTestcase() {
            Testcase hiddenTestcase = new Testcase(problem, "[4,5,6]", "15", true);
            
            assertThat(hiddenTestcase.isHidden()).isTrue();
        }

        @Test
        @DisplayName("should have createdAt set to current time")
        void shouldHaveCreatedAtSetToCurrentTime() {
            LocalDateTime now = LocalDateTime.now();
            assertThat(testcase.getCreatedAt()).isNotNull();
            assertThat(testcase.getCreatedAt()).isBeforeOrEqualTo(now);
        }

        @Test
        @DisplayName("should have default isHidden as false")
        void shouldHaveDefaultIsHiddenAsFalse() {
            Testcase defaultTestcase = new Testcase();
            assertThat(defaultTestcase.isHidden()).isFalse();
        }

        @Test
        @DisplayName("should create testcase with default constructor")
        void shouldCreateTestcaseWithDefaultConstructor() {
            Testcase defaultTestcase = new Testcase();
            assertThat(defaultTestcase.getId()).isNull();
            assertThat(defaultTestcase.getProblem()).isNull();
            assertThat(defaultTestcase.getInput()).isNull();
            assertThat(defaultTestcase.getExpectedOutput()).isNull();
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("should set and get id")
        void shouldSetAndGetId() {
            testcase.setId(100L);
            assertThat(testcase.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should set and get problem")
        void shouldSetAndGetProblem() {
            Problem newProblem = new Problem();
            newProblem.setId(2L);
            
            testcase.setProblem(newProblem);
            assertThat(testcase.getProblem()).isEqualTo(newProblem);
        }

        @Test
        @DisplayName("should set and get input")
        void shouldSetAndGetInput() {
            testcase.setInput("[7,8,9]");
            assertThat(testcase.getInput()).isEqualTo("[7,8,9]");
        }

        @Test
        @DisplayName("should set and get expectedOutput")
        void shouldSetAndGetExpectedOutput() {
            testcase.setExpectedOutput("24");
            assertThat(testcase.getExpectedOutput()).isEqualTo("24");
        }

        @Test
        @DisplayName("should set and get isHidden")
        void shouldSetAndGetIsHidden() {
            testcase.setHidden(true);
            assertThat(testcase.isHidden()).isTrue();
            
            testcase.setHidden(false);
            assertThat(testcase.isHidden()).isFalse();
        }

        @Test
        @DisplayName("should set and get createdAt")
        void shouldSetAndGetCreatedAt() {
            LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30);
            testcase.setCreatedAt(customTime);
            assertThat(testcase.getCreatedAt()).isEqualTo(customTime);
        }
    }

    @Nested
    @DisplayName("Multiline Input/Output")
    class MultilineInputOutput {

        @Test
        @DisplayName("should handle multiline input")
        void shouldHandleMultilineInput() {
            String multilineInput = "5\n1 2 3 4 5\n3";
            testcase.setInput(multilineInput);
            assertThat(testcase.getInput()).isEqualTo(multilineInput);
        }

        @Test
        @DisplayName("should handle multiline expected output")
        void shouldHandleMultilineExpectedOutput() {
            String multilineOutput = "1\n2\n3\n4\n5";
            testcase.setExpectedOutput(multilineOutput);
            assertThat(testcase.getExpectedOutput()).isEqualTo(multilineOutput);
        }

        @Test
        @DisplayName("should handle empty input")
        void shouldHandleEmptyInput() {
            testcase.setInput("");
            assertThat(testcase.getInput()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty expected output")
        void shouldHandleEmptyExpectedOutput() {
            testcase.setExpectedOutput("");
            assertThat(testcase.getExpectedOutput()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            testcase.setId(1L);
            assertThat(testcase).isEqualTo(testcase);
        }

        @Test
        @DisplayName("should be equal to another testcase with same id")
        void shouldBeEqualToAnotherTestcaseWithSameId() {
            testcase.setId(1L);
            
            Testcase other = new Testcase();
            other.setId(1L);
            
            assertThat(testcase).isEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to testcase with different id")
        void shouldNotBeEqualToTestcaseWithDifferentId() {
            testcase.setId(1L);
            
            Testcase other = new Testcase();
            other.setId(2L);
            
            assertThat(testcase).isNotEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            testcase.setId(1L);
            assertThat(testcase).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            testcase.setId(1L);
            assertThat(testcase).isNotEqualTo("not a testcase");
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            testcase.setId(1L);
            int hashCode1 = testcase.hashCode();
            int hashCode2 = testcase.hashCode();
            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        @DisplayName("should not be equal when id is null")
        void shouldNotBeEqualWhenIdIsNull() {
            Testcase other = new Testcase();
            assertThat(testcase).isNotEqualTo(other);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        @DisplayName("should include key fields in toString")
        void shouldIncludeKeyFieldsInToString() {
            testcase.setId(1L);
            String result = testcase.toString();
            
            assertThat(result).contains("id=1");
            assertThat(result).contains("isHidden=false");
        }

        @Test
        @DisplayName("should not include sensitive data in toString")
        void shouldNotIncludeSensitiveDataInToString() {
            testcase.setId(1L);
            String result = testcase.toString();
            
            // Input and expectedOutput should not be in toString to avoid exposing hidden testcase data
            assertThat(result).doesNotContain("[1,2,3]");
            assertThat(result).doesNotContain("expectedOutput");
        }
    }
}
