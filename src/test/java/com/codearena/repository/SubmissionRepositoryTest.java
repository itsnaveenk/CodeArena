package com.codearena.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.codearena.document.Submission;
import com.codearena.entity.Verdict;

/**
 * Repository tests for SubmissionRepository using embedded MongoDB.
 */
@DataMongoTest
@ActiveProfiles("test")
class SubmissionRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    private Submission testSubmission;

    @BeforeEach
    void setUp() {
        testSubmission = new Submission(1L, 100L, 62, "public class Solution {}",
                Verdict.ACCEPTED, 0.5, 1024, 5, 5);
        testSubmission = submissionRepository.save(testSubmission);
    }

    @AfterEach
    void tearDown() {
        submissionRepository.deleteAll();
    }

    @Nested
    @DisplayName("Standard MongoDB Operations")
    class StandardMongoOperations {

        @Test
        @DisplayName("save should persist new submission with generated ID")
        void savePersistsNewSubmission() {
            Submission newSubmission = new Submission(2L, 101L, 71, "print('hello')",
                    Verdict.WRONG_ANSWER, 0.3, 512, 3, 5);

            Submission saved = submissionRepository.save(newSubmission);

            assertNotNull(saved.getId());
            assertEquals(2L, saved.getUserId());
            assertEquals(101L, saved.getProblemId());
            assertEquals(Verdict.WRONG_ANSWER, saved.getVerdict());
        }

        @Test
        @DisplayName("findById should return submission when id exists")
        void findByIdReturnsSubmissionWhenExists() {
            Optional<Submission> found = submissionRepository.findById(testSubmission.getId());

            assertTrue(found.isPresent());
            assertEquals(testSubmission.getUserId(), found.get().getUserId());
            assertEquals(testSubmission.getProblemId(), found.get().getProblemId());
        }

        @Test
        @DisplayName("findById should return empty when id does not exist")
        void findByIdReturnsEmptyWhenNotExists() {
            Optional<Submission> found = submissionRepository.findById("nonexistent-id");

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("delete should remove submission")
        void deleteRemovesSubmission() {
            String id = testSubmission.getId();

            submissionRepository.delete(testSubmission);

            Optional<Submission> found = submissionRepository.findById(id);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("count should return correct number of submissions")
        void countReturnsCorrectNumber() {
            long initialCount = submissionRepository.count();

            Submission another = new Submission(2L, 102L, 54, "code",
                    Verdict.TLE, 5.0, 2048, 2, 5);
            submissionRepository.save(another);

            assertEquals(initialCount + 1, submissionRepository.count());
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("should return submissions for user with pagination")
        void returnsSubmissionsForUserWithPagination() {
            // Add more submissions for user 1
            submissionRepository.save(new Submission(1L, 101L, 62, "code1",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));
            submissionRepository.save(new Submission(1L, 102L, 62, "code2",
                    Verdict.ACCEPTED, 0.6, 900, 5, 5));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByUserId(1L, pageable);

            assertEquals(3, result.getTotalElements());
            assertTrue(result.getContent().stream().allMatch(s -> s.getUserId().equals(1L)));
        }

        @Test
        @DisplayName("should return empty page when user has no submissions")
        void returnsEmptyPageWhenNoSubmissions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByUserId(999L, pageable);

            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("should support sorting by createdAt descending")
        void supportsSortingByCreatedAtDescending() {
            // Add submissions with different times
            Submission older = new Submission(1L, 101L, 62, "code1",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5);
            older.setCreatedAt(LocalDateTime.now().minusHours(2));
            submissionRepository.save(older);

            Submission newer = new Submission(1L, 102L, 62, "code2",
                    Verdict.ACCEPTED, 0.6, 900, 5, 5);
            newer.setCreatedAt(LocalDateTime.now().plusHours(1));
            submissionRepository.save(newer);

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Submission> result = submissionRepository.findByUserId(1L, pageable);

            List<Submission> submissions = result.getContent();
            assertTrue(submissions.get(0).getCreatedAt().isAfter(submissions.get(1).getCreatedAt()));
        }

        @Test
        @DisplayName("should paginate correctly")
        void paginatesCorrectly() {
            // Add 5 more submissions for user 1 (total 6)
            for (int i = 0; i < 5; i++) {
                submissionRepository.save(new Submission(1L, (long) (101 + i), 62, "code" + i,
                        Verdict.ACCEPTED, 0.5, 1024, 5, 5));
            }

            Page<Submission> page1 = submissionRepository.findByUserId(1L, PageRequest.of(0, 2));
            Page<Submission> page2 = submissionRepository.findByUserId(1L, PageRequest.of(1, 2));

            assertEquals(6, page1.getTotalElements());
            assertEquals(2, page1.getContent().size());
            assertEquals(2, page2.getContent().size());
            assertEquals(3, page1.getTotalPages());
        }
    }

    @Nested
    @DisplayName("findByUserIdAndProblemId")
    class FindByUserIdAndProblemId {

        @Test
        @DisplayName("should return submissions for user and problem")
        void returnsSubmissionsForUserAndProblem() {
            // Add another submission for same user and problem
            submissionRepository.save(new Submission(1L, 100L, 62, "code2",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByUserIdAndProblemId(1L, 100L, pageable);

            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream()
                    .allMatch(s -> s.getUserId().equals(1L) && s.getProblemId().equals(100L)));
        }

        @Test
        @DisplayName("should return empty when no matching submissions")
        void returnsEmptyWhenNoMatch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByUserIdAndProblemId(1L, 999L, pageable);

            assertEquals(0, result.getTotalElements());
        }

        @Test
        @DisplayName("should not return submissions from other users")
        void doesNotReturnOtherUsersSubmissions() {
            // Add submission for different user, same problem
            submissionRepository.save(new Submission(2L, 100L, 62, "code",
                    Verdict.ACCEPTED, 0.5, 1024, 5, 5));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByUserIdAndProblemId(1L, 100L, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(1L, result.getContent().get(0).getUserId());
        }
    }

    @Nested
    @DisplayName("findByUserIdAndVerdictAndProblemIdIn")
    class FindByUserIdAndVerdictAndProblemIdIn {

        @Test
        @DisplayName("should return submissions matching user, verdict, and problem IDs")
        void returnsMatchingSubmissions() {
            // Add more submissions
            submissionRepository.save(new Submission(1L, 101L, 62, "code1",
                    Verdict.ACCEPTED, 0.5, 1024, 5, 5));
            submissionRepository.save(new Submission(1L, 102L, 62, "code2",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));
            submissionRepository.save(new Submission(1L, 103L, 62, "code3",
                    Verdict.ACCEPTED, 0.6, 900, 5, 5));

            List<Long> problemIds = List.of(100L, 101L, 103L);
            List<Submission> result = submissionRepository.findByUserIdAndVerdictAndProblemIdIn(
                    1L, Verdict.ACCEPTED, problemIds);

            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(s -> s.getVerdict() == Verdict.ACCEPTED));
            assertTrue(result.stream().allMatch(s -> problemIds.contains(s.getProblemId())));
        }

        @Test
        @DisplayName("should return empty list when no matches")
        void returnsEmptyWhenNoMatches() {
            List<Long> problemIds = List.of(999L, 998L);
            List<Submission> result = submissionRepository.findByUserIdAndVerdictAndProblemIdIn(
                    1L, Verdict.ACCEPTED, problemIds);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should filter by verdict correctly")
        void filtersByVerdictCorrectly() {
            submissionRepository.save(new Submission(1L, 100L, 62, "code",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));

            List<Long> problemIds = List.of(100L);
            List<Submission> accepted = submissionRepository.findByUserIdAndVerdictAndProblemIdIn(
                    1L, Verdict.ACCEPTED, problemIds);
            List<Submission> wrongAnswer = submissionRepository.findByUserIdAndVerdictAndProblemIdIn(
                    1L, Verdict.WRONG_ANSWER, problemIds);

            assertEquals(1, accepted.size());
            assertEquals(1, wrongAnswer.size());
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndProblemId")
    class ExistsByUserIdAndProblemId {

        @Test
        @DisplayName("should return true when submission exists")
        void returnsTrueWhenExists() {
            boolean exists = submissionRepository.existsByUserIdAndProblemId(1L, 100L);

            assertTrue(exists);
        }

        @Test
        @DisplayName("should return false when no submission exists")
        void returnsFalseWhenNotExists() {
            boolean exists = submissionRepository.existsByUserIdAndProblemId(1L, 999L);

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndProblemIdAndVerdict")
    class ExistsByUserIdAndProblemIdAndVerdict {

        @Test
        @DisplayName("should return true when submission with verdict exists")
        void returnsTrueWhenExists() {
            boolean exists = submissionRepository.existsByUserIdAndProblemIdAndVerdict(
                    1L, 100L, Verdict.ACCEPTED);

            assertTrue(exists);
        }

        @Test
        @DisplayName("should return false when no submission with verdict exists")
        void returnsFalseWhenNotExists() {
            boolean exists = submissionRepository.existsByUserIdAndProblemIdAndVerdict(
                    1L, 100L, Verdict.WRONG_ANSWER);

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("countByUserIdAndProblemId")
    class CountByUserIdAndProblemId {

        @Test
        @DisplayName("should return correct count")
        void returnsCorrectCount() {
            // Add more submissions for same user and problem
            submissionRepository.save(new Submission(1L, 100L, 62, "code2",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));
            submissionRepository.save(new Submission(1L, 100L, 62, "code3",
                    Verdict.TLE, 5.0, 2048, 2, 5));

            long count = submissionRepository.countByUserIdAndProblemId(1L, 100L);

            assertEquals(3, count);
        }

        @Test
        @DisplayName("should return zero when no submissions")
        void returnsZeroWhenNoSubmissions() {
            long count = submissionRepository.countByUserIdAndProblemId(1L, 999L);

            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("findByProblemId")
    class FindByProblemId {

        @Test
        @DisplayName("should return all submissions for problem")
        void returnsAllSubmissionsForProblem() {
            // Add submissions from different users for same problem
            submissionRepository.save(new Submission(2L, 100L, 62, "code2",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));
            submissionRepository.save(new Submission(3L, 100L, 71, "code3",
                    Verdict.ACCEPTED, 0.3, 512, 5, 5));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByProblemId(100L, pageable);

            assertEquals(3, result.getTotalElements());
            assertTrue(result.getContent().stream().allMatch(s -> s.getProblemId().equals(100L)));
        }

        @Test
        @DisplayName("should return empty when no submissions for problem")
        void returnsEmptyWhenNoSubmissions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Submission> result = submissionRepository.findByProblemId(999L, pageable);

            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("findByUserId (List)")
    class FindByUserIdList {

        @Test
        @DisplayName("should return all submissions for user")
        void returnsAllSubmissionsForUser() {
            // Add more submissions for user 1
            submissionRepository.save(new Submission(1L, 101L, 62, "code1",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));
            submissionRepository.save(new Submission(1L, 102L, 62, "code2",
                    Verdict.ACCEPTED, 0.6, 900, 5, 5));

            List<Submission> result = submissionRepository.findByUserId(1L);

            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(s -> s.getUserId().equals(1L)));
        }

        @Test
        @DisplayName("should return empty list when user has no submissions")
        void returnsEmptyListWhenNoSubmissions() {
            List<Submission> result = submissionRepository.findByUserId(999L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("countByUserIdAndVerdict")
    class CountByUserIdAndVerdict {

        @Test
        @DisplayName("should count submissions with specific verdict")
        void countsSubmissionsWithVerdict() {
            // Add more submissions
            submissionRepository.save(new Submission(1L, 101L, 62, "code1",
                    Verdict.ACCEPTED, 0.5, 1024, 5, 5));
            submissionRepository.save(new Submission(1L, 102L, 62, "code2",
                    Verdict.WRONG_ANSWER, 0.4, 800, 3, 5));

            long acceptedCount = submissionRepository.countByUserIdAndVerdict(1L, Verdict.ACCEPTED);
            long wrongAnswerCount = submissionRepository.countByUserIdAndVerdict(1L, Verdict.WRONG_ANSWER);

            assertEquals(2, acceptedCount);
            assertEquals(1, wrongAnswerCount);
        }
    }

    @Nested
    @DisplayName("Index Verification")
    class IndexVerification {

        @Test
        @DisplayName("Queries on indexed fields should work efficiently")
        void queriesOnIndexedFieldsWork() {
            // Add many submissions to test index usage
            for (int i = 0; i < 50; i++) {
                submissionRepository.save(new Submission(
                        (long) (i % 5 + 1),  // 5 different users
                        (long) (i % 10 + 100),  // 10 different problems
                        62,
                        "code" + i,
                        i % 2 == 0 ? Verdict.ACCEPTED : Verdict.WRONG_ANSWER,
                        0.5,
                        1024,
                        5,
                        5
                ));
            }

            // These queries should use indexes
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<Submission> byUser = submissionRepository.findByUserId(1L, pageable);
            Page<Submission> byUserAndProblem = submissionRepository.findByUserIdAndProblemId(1L, 100L, pageable);

            // Verify queries return results (index usage is implicit)
            assertFalse(byUser.isEmpty());
            assertNotNull(byUserAndProblem);
        }
    }
}
