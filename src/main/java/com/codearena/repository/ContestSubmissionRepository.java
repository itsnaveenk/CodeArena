package com.codearena.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.codearena.document.ContestSubmission;

@Repository
public interface ContestSubmissionRepository extends MongoRepository<ContestSubmission, String> {

    Page<ContestSubmission> findByContestIdAndUserId(Long contestId, Long userId, Pageable pageable);

    Page<ContestSubmission> findByContestIdAndUserIdAndProblemId(
        Long contestId, Long userId, Long problemId, Pageable pageable);

    @Query("{ 'contestId': ?0, 'userId': ?1, 'problemId': ?2 }")
    List<ContestSubmission> findByContestIdAndUserIdAndProblemIdOrderByPointsEarnedDesc(
        Long contestId, Long userId, Long problemId);

    long countByContestIdAndUserIdAndSubmittedAtAfter(Long contestId, Long userId, LocalDateTime after);

    long countByContestIdAndUserIdAndProblemIdAndSubmittedAtAfter(
        Long contestId, Long userId, Long problemId, LocalDateTime after);

    long countByContestId(Long contestId);

    @Query(value = "{ 'contestId': ?0 }", count = true)
    long countSubmissionsByContestId(Long contestId);

    List<ContestSubmission> findByContestIdAndProblemId(Long contestId, Long problemId);

    Optional<ContestSubmission> findFirstByContestIdAndUserIdAndProblemIdOrderByPointsEarnedDesc(
        Long contestId, Long userId, Long problemId);

    boolean existsByContestIdAndUserIdAndProblemId(Long contestId, Long userId, Long problemId);
}
