package com.codearena.document;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.codearena.entity.Verdict;

/**
 * Unit tests for the Submission MongoDB document class.
 */
class SubmissionTest {

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Default constructor should create submission with null fields and default createdAt")
        void defaultConstructorCreatesSubmissionWithDefaults() {
            Submission submission = new Submission();

            assertNull(submission.getId());
            assertNull(submission.getUserId());
            assertNull(submission.getProblemId());
            assertNull(submission.getLanguageId());
            assertNull(submission.getCode());
            assertNull(submission.getVerdict());
            assertNull(submission.getRuntime());
            assertNull(submission.getMemory());
            assertEquals(0, submission.getPassedTestcases());
            assertEquals(0, submission.getTotalTestcases());
            assertNotNull(submission.getCreatedAt());
        }

        @Test
        @DisplayName("Parameterized constructor should set all fields correctly")
        void parameterizedConstructorSetsAllFields() {
            Long userId = 1L;
            Long problemId = 2L;
            Integer languageId = 62;
            String code = "public class Solution {}";
            Verdict verdict = Verdict.ACCEPTED;
            Double runtime = 0.5;
            Integer memory = 1024;
            int passedTestcases = 5;
            int totalTestcases = 5;

            Submission submission = new Submission(userId, problemId, languageId, code,
                    verdict, runtime, memory, passedTestcases, totalTestcases);

            assertNull(submission.getId()); // ID is set by MongoDB
            assertEquals(userId, submission.getUserId());
            assertEquals(problemId, submission.getProblemId());
            assertEquals(languageId, submission.getLanguageId());
            assertEquals(code, submission.getCode());
            assertEquals(verdict, submission.getVerdict());
            assertEquals(runtime, submission.getRuntime());
            assertEquals(memory, submission.getMemory());
            assertEquals(passedTestcases, submission.getPassedTestcases());
            assertEquals(totalTestcases, submission.getTotalTestcases());
            assertNotNull(submission.getCreatedAt());
        }

        @Test
        @DisplayName("createdAt should be set to current time by default")
        void createdAtIsSetToCurrentTime() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            Submission submission = new Submission();
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertTrue(submission.getCreatedAt().isAfter(before));
            assertTrue(submission.getCreatedAt().isBefore(after));
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("setId should update id")
        void setIdUpdatesId() {
            Submission submission = new Submission();
            submission.setId("507f1f77bcf86cd799439011");

            assertEquals("507f1f77bcf86cd799439011", submission.getId());
        }

        @Test
        @DisplayName("setUserId should update userId")
        void setUserIdUpdatesUserId() {
            Submission submission = new Submission();
            submission.setUserId(123L);

            assertEquals(123L, submission.getUserId());
        }

        @Test
        @DisplayName("setProblemId should update problemId")
        void setProblemIdUpdatesProblemId() {
            Submission submission = new Submission();
            submission.setProblemId(456L);

            assertEquals(456L, submission.getProblemId());
        }

        @Test
        @DisplayName("setLanguageId should update languageId")
        void setLanguageIdUpdatesLanguageId() {
            Submission submission = new Submission();
            submission.setLanguageId(71);

            assertEquals(71, submission.getLanguageId());
        }

        @Test
        @DisplayName("setCode should update code")
        void setCodeUpdatesCode() {
            Submission submission = new Submission();
            String code = "print('Hello, World!')";
            submission.setCode(code);

            assertEquals(code, submission.getCode());
        }

        @Test
        @DisplayName("setVerdict should update verdict")
        void setVerdictUpdatesVerdict() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.WRONG_ANSWER);

            assertEquals(Verdict.WRONG_ANSWER, submission.getVerdict());
        }

        @Test
        @DisplayName("setRuntime should update runtime")
        void setRuntimeUpdatesRuntime() {
            Submission submission = new Submission();
            submission.setRuntime(1.234);

            assertEquals(1.234, submission.getRuntime());
        }

        @Test
        @DisplayName("setMemory should update memory")
        void setMemoryUpdatesMemory() {
            Submission submission = new Submission();
            submission.setMemory(2048);

            assertEquals(2048, submission.getMemory());
        }

        @Test
        @DisplayName("setPassedTestcases should update passedTestcases")
        void setPassedTestcasesUpdatesPassedTestcases() {
            Submission submission = new Submission();
            submission.setPassedTestcases(3);

            assertEquals(3, submission.getPassedTestcases());
        }

        @Test
        @DisplayName("setTotalTestcases should update totalTestcases")
        void setTotalTestcasesUpdatesTotalTestcases() {
            Submission submission = new Submission();
            submission.setTotalTestcases(5);

            assertEquals(5, submission.getTotalTestcases());
        }

        @Test
        @DisplayName("setCreatedAt should update createdAt")
        void setCreatedAtUpdatesCreatedAt() {
            Submission submission = new Submission();
            LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            submission.setCreatedAt(customTime);

            assertEquals(customTime, submission.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Verdict Values")
    class VerdictValues {

        @Test
        @DisplayName("Submission can have ACCEPTED verdict")
        void submissionCanHaveAcceptedVerdict() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.ACCEPTED);

            assertEquals(Verdict.ACCEPTED, submission.getVerdict());
        }

        @Test
        @DisplayName("Submission can have WRONG_ANSWER verdict")
        void submissionCanHaveWrongAnswerVerdict() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.WRONG_ANSWER);

            assertEquals(Verdict.WRONG_ANSWER, submission.getVerdict());
        }

        @Test
        @DisplayName("Submission can have TLE verdict")
        void submissionCanHaveTleVerdict() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.TLE);

            assertEquals(Verdict.TLE, submission.getVerdict());
        }

        @Test
        @DisplayName("Submission can have RUNTIME_ERROR verdict")
        void submissionCanHaveRuntimeErrorVerdict() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.RUNTIME_ERROR);

            assertEquals(Verdict.RUNTIME_ERROR, submission.getVerdict());
        }

        @Test
        @DisplayName("Submission can have COMPILATION_ERROR verdict")
        void submissionCanHaveCompilationErrorVerdict() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.COMPILATION_ERROR);

            assertEquals(Verdict.COMPILATION_ERROR, submission.getVerdict());
        }
    }

    @Nested
    @DisplayName("Language IDs")
    class LanguageIds {

        @Test
        @DisplayName("Submission can have Java language ID (62)")
        void submissionCanHaveJavaLanguageId() {
            Submission submission = new Submission();
            submission.setLanguageId(62);

            assertEquals(62, submission.getLanguageId());
        }

        @Test
        @DisplayName("Submission can have Python language ID (71)")
        void submissionCanHavePythonLanguageId() {
            Submission submission = new Submission();
            submission.setLanguageId(71);

            assertEquals(71, submission.getLanguageId());
        }

        @Test
        @DisplayName("Submission can have C++ language ID (54)")
        void submissionCanHaveCppLanguageId() {
            Submission submission = new Submission();
            submission.setLanguageId(54);

            assertEquals(54, submission.getLanguageId());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Submissions with same id should be equal")
        void submissionsWithSameIdAreEqual() {
            Submission submission1 = new Submission();
            submission1.setId("507f1f77bcf86cd799439011");

            Submission submission2 = new Submission();
            submission2.setId("507f1f77bcf86cd799439011");

            assertEquals(submission1, submission2);
        }

        @Test
        @DisplayName("Submissions with different ids should not be equal")
        void submissionsWithDifferentIdsAreNotEqual() {
            Submission submission1 = new Submission();
            submission1.setId("507f1f77bcf86cd799439011");

            Submission submission2 = new Submission();
            submission2.setId("507f1f77bcf86cd799439012");

            assertNotEquals(submission1, submission2);
        }

        @Test
        @DisplayName("Submissions with null ids should not be equal")
        void submissionsWithNullIdsAreNotEqual() {
            Submission submission1 = new Submission();
            Submission submission2 = new Submission();

            // Both have null IDs, so they should not be equal based on the equals implementation
            assertNotEquals(submission1, submission2);
        }

        @Test
        @DisplayName("Submission should be equal to itself")
        void submissionIsEqualToItself() {
            Submission submission = new Submission();
            submission.setId("507f1f77bcf86cd799439011");

            assertEquals(submission, submission);
        }

        @Test
        @DisplayName("Submission should not be equal to null")
        void submissionIsNotEqualToNull() {
            Submission submission = new Submission();
            submission.setId("507f1f77bcf86cd799439011");

            assertNotEquals(null, submission);
        }

        @Test
        @DisplayName("Submission should not be equal to different type")
        void submissionIsNotEqualToDifferentType() {
            Submission submission = new Submission();
            submission.setId("507f1f77bcf86cd799439011");

            assertNotEquals("507f1f77bcf86cd799439011", submission);
        }

        @Test
        @DisplayName("Equal submissions should have same hashCode")
        void equalSubmissionsHaveSameHashCode() {
            Submission submission1 = new Submission();
            submission1.setId("507f1f77bcf86cd799439011");

            Submission submission2 = new Submission();
            submission2.setId("507f1f77bcf86cd799439011");

            assertEquals(submission1.hashCode(), submission2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        @DisplayName("toString should include all relevant fields")
        void toStringIncludesAllFields() {
            Submission submission = new Submission(1L, 2L, 62, "code",
                    Verdict.ACCEPTED, 0.5, 1024, 5, 5);
            submission.setId("507f1f77bcf86cd799439011");

            String result = submission.toString();

            assertTrue(result.contains("id='507f1f77bcf86cd799439011'"));
            assertTrue(result.contains("userId=1"));
            assertTrue(result.contains("problemId=2"));
            assertTrue(result.contains("languageId=62"));
            assertTrue(result.contains("verdict=ACCEPTED"));
            assertTrue(result.contains("runtime=0.5"));
            assertTrue(result.contains("memory=1024"));
            assertTrue(result.contains("passedTestcases=5"));
            assertTrue(result.contains("totalTestcases=5"));
            assertTrue(result.contains("createdAt="));
        }

        @Test
        @DisplayName("toString should not include code field for brevity")
        void toStringDoesNotIncludeCode() {
            Submission submission = new Submission(1L, 2L, 62, "very long code here",
                    Verdict.ACCEPTED, 0.5, 1024, 5, 5);

            String result = submission.toString();

            // Code is intentionally excluded from toString for brevity
            assertFalse(result.contains("very long code here"));
        }
    }

    @Nested
    @DisplayName("Testcase Counts")
    class TestcaseCounts {

        @Test
        @DisplayName("Passed testcases can be zero")
        void passedTestcasesCanBeZero() {
            Submission submission = new Submission(1L, 2L, 62, "code",
                    Verdict.WRONG_ANSWER, 0.5, 1024, 0, 5);

            assertEquals(0, submission.getPassedTestcases());
            assertEquals(5, submission.getTotalTestcases());
        }

        @Test
        @DisplayName("Passed testcases can equal total testcases")
        void passedTestcasesCanEqualTotal() {
            Submission submission = new Submission(1L, 2L, 62, "code",
                    Verdict.ACCEPTED, 0.5, 1024, 10, 10);

            assertEquals(10, submission.getPassedTestcases());
            assertEquals(10, submission.getTotalTestcases());
        }

        @Test
        @DisplayName("Partial testcase pass is valid")
        void partialTestcasePassIsValid() {
            Submission submission = new Submission(1L, 2L, 62, "code",
                    Verdict.WRONG_ANSWER, 0.5, 1024, 3, 5);

            assertEquals(3, submission.getPassedTestcases());
            assertEquals(5, submission.getTotalTestcases());
        }
    }

    @Nested
    @DisplayName("Nullable Fields")
    class NullableFields {

        @Test
        @DisplayName("Runtime can be null for compilation errors")
        void runtimeCanBeNullForCompilationErrors() {
            Submission submission = new Submission(1L, 2L, 62, "invalid code",
                    Verdict.COMPILATION_ERROR, null, null, 0, 5);

            assertNull(submission.getRuntime());
            assertNull(submission.getMemory());
            assertEquals(Verdict.COMPILATION_ERROR, submission.getVerdict());
        }

        @Test
        @DisplayName("Memory can be null for compilation errors")
        void memoryCanBeNullForCompilationErrors() {
            Submission submission = new Submission();
            submission.setVerdict(Verdict.COMPILATION_ERROR);
            submission.setMemory(null);

            assertNull(submission.getMemory());
        }
    }
}
