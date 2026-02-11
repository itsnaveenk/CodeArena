package com.codearena.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StarterCode} entity.
 * Tests entity creation, properties, and relationships.
 */
@DisplayName("StarterCode Entity")
class StarterCodeTest {

    private User testUser;
    private Problem problem;
    private StarterCode starterCode;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "hashedPassword", Role.PROBLEM_SETTER);
        testUser.setId(1L);
        
        problem = new Problem("Two Sum", "two-sum", "Find two numbers that add up to target", 
                Difficulty.EASY, testUser);
        problem.setId(1L);
        
        starterCode = new StarterCode(problem, SupportedLanguage.JAVA.getJudge0Id(), 
                "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // Your code here\n    }\n}");
    }

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("should create starter code with all fields via constructor")
        void shouldCreateStarterCodeWithAllFieldsViaConstructor() {
            assertThat(starterCode.getProblem()).isEqualTo(problem);
            assertThat(starterCode.getLanguageId()).isEqualTo(62); // Java
            assertThat(starterCode.getCode()).contains("class Solution");
        }

        @Test
        @DisplayName("should create starter code for Python")
        void shouldCreateStarterCodeForPython() {
            StarterCode pythonCode = new StarterCode(problem, SupportedLanguage.PYTHON.getJudge0Id(), 
                    "class Solution:\n    def twoSum(self, nums, target):\n        pass");
            
            assertThat(pythonCode.getLanguageId()).isEqualTo(71); // Python
            assertThat(pythonCode.getCode()).contains("class Solution:");
        }

        @Test
        @DisplayName("should create starter code for C++")
        void shouldCreateStarterCodeForCpp() {
            StarterCode cppCode = new StarterCode(problem, SupportedLanguage.CPP.getJudge0Id(), 
                    "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n    }\n};");
            
            assertThat(cppCode.getLanguageId()).isEqualTo(54); // C++
            assertThat(cppCode.getCode()).contains("vector<int>");
        }

        @Test
        @DisplayName("should create starter code with default constructor")
        void shouldCreateStarterCodeWithDefaultConstructor() {
            StarterCode defaultCode = new StarterCode();
            assertThat(defaultCode.getId()).isNull();
            assertThat(defaultCode.getProblem()).isNull();
            assertThat(defaultCode.getLanguageId()).isNull();
            assertThat(defaultCode.getCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("should set and get id")
        void shouldSetAndGetId() {
            starterCode.setId(100L);
            assertThat(starterCode.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should set and get problem")
        void shouldSetAndGetProblem() {
            Problem newProblem = new Problem();
            newProblem.setId(2L);
            
            starterCode.setProblem(newProblem);
            assertThat(starterCode.getProblem()).isEqualTo(newProblem);
        }

        @Test
        @DisplayName("should set and get languageId")
        void shouldSetAndGetLanguageId() {
            starterCode.setLanguageId(71); // Python
            assertThat(starterCode.getLanguageId()).isEqualTo(71);
        }

        @Test
        @DisplayName("should set and get code")
        void shouldSetAndGetCode() {
            String newCode = "def solution(): pass";
            starterCode.setCode(newCode);
            assertThat(starterCode.getCode()).isEqualTo(newCode);
        }
    }

    @Nested
    @DisplayName("Code Content")
    class CodeContent {

        @Test
        @DisplayName("should handle multiline code")
        void shouldHandleMultilineCode() {
            String multilineCode = """
                    class Solution {
                        public int[] twoSum(int[] nums, int target) {
                            Map<Integer, Integer> map = new HashMap<>();
                            for (int i = 0; i < nums.length; i++) {
                                int complement = target - nums[i];
                                if (map.containsKey(complement)) {
                                    return new int[] { map.get(complement), i };
                                }
                                map.put(nums[i], i);
                            }
                            return new int[] {};
                        }
                    }
                    """;
            starterCode.setCode(multilineCode);
            assertThat(starterCode.getCode()).contains("HashMap");
            assertThat(starterCode.getCode()).contains("complement");
        }

        @Test
        @DisplayName("should handle empty code")
        void shouldHandleEmptyCode() {
            starterCode.setCode("");
            assertThat(starterCode.getCode()).isEmpty();
        }

        @Test
        @DisplayName("should handle code with special characters")
        void shouldHandleCodeWithSpecialCharacters() {
            String codeWithSpecialChars = "// Comment with special chars: <>&\"'\n" +
                    "String s = \"Hello\\nWorld\";";
            starterCode.setCode(codeWithSpecialChars);
            assertThat(starterCode.getCode()).isEqualTo(codeWithSpecialChars);
        }
    }

    @Nested
    @DisplayName("Language IDs")
    class LanguageIds {

        @Test
        @DisplayName("should accept Java language ID")
        void shouldAcceptJavaLanguageId() {
            starterCode.setLanguageId(62);
            assertThat(starterCode.getLanguageId()).isEqualTo(62);
        }

        @Test
        @DisplayName("should accept Python language ID")
        void shouldAcceptPythonLanguageId() {
            starterCode.setLanguageId(71);
            assertThat(starterCode.getLanguageId()).isEqualTo(71);
        }

        @Test
        @DisplayName("should accept C++ language ID")
        void shouldAcceptCppLanguageId() {
            starterCode.setLanguageId(54);
            assertThat(starterCode.getLanguageId()).isEqualTo(54);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            starterCode.setId(1L);
            assertThat(starterCode).isEqualTo(starterCode);
        }

        @Test
        @DisplayName("should be equal to another starter code with same id")
        void shouldBeEqualToAnotherStarterCodeWithSameId() {
            starterCode.setId(1L);
            
            StarterCode other = new StarterCode();
            other.setId(1L);
            
            assertThat(starterCode).isEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to starter code with different id")
        void shouldNotBeEqualToStarterCodeWithDifferentId() {
            starterCode.setId(1L);
            
            StarterCode other = new StarterCode();
            other.setId(2L);
            
            assertThat(starterCode).isNotEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            starterCode.setId(1L);
            assertThat(starterCode).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            starterCode.setId(1L);
            assertThat(starterCode).isNotEqualTo("not a starter code");
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            starterCode.setId(1L);
            int hashCode1 = starterCode.hashCode();
            int hashCode2 = starterCode.hashCode();
            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        @DisplayName("should not be equal when id is null")
        void shouldNotBeEqualWhenIdIsNull() {
            StarterCode other = new StarterCode();
            assertThat(starterCode).isNotEqualTo(other);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        @DisplayName("should include key fields in toString")
        void shouldIncludeKeyFieldsInToString() {
            starterCode.setId(1L);
            String result = starterCode.toString();
            
            assertThat(result).contains("id=1");
            assertThat(result).contains("languageId=62");
        }

        @Test
        @DisplayName("should not include code content in toString")
        void shouldNotIncludeCodeContentInToString() {
            starterCode.setId(1L);
            String result = starterCode.toString();
            
            // Code content should not be in toString to keep it concise
            assertThat(result).doesNotContain("class Solution");
        }
    }
}
