package com.codearena.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.codearena.document.Submission;
import com.codearena.entity.Verdict;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Page<Submission> findByUserId(Long userId, Pageable pageable);

    Page<Submission> findByUserIdAndProblemId(Long userId, Long problemId, Pageable pageable);

    List<Submission> findByUserIdAndVerdictAndProblemIdIn(Long userId, Verdict verdict, List<Long> problemIds);

    List<Submission> findByUserId(Long userId);

    @Query(value = "{ 'userId': ?0, 'verdict': ?1 }", count = true)
    long countByUserIdAndVerdict(Long userId, Verdict verdict);

    @Query(value = "{ 'userId': ?0, 'verdict': ?1 }", fields = "{ 'problemId': 1 }")
    List<Submission> findDistinctProblemIdsByUserIdAndVerdict(Long userId, Verdict verdict);

    @Query(value = "{ 'userId': ?0 }", fields = "{ 'problemId': 1 }")
    List<Submission> findDistinctProblemIdsByUserId(Long userId);

    boolean existsByUserIdAndProblemId(Long userId, Long problemId);

    boolean existsByUserIdAndProblemIdAndVerdict(Long userId, Long problemId, Verdict verdict);

    long countByUserIdAndProblemId(Long userId, Long problemId);

    Page<Submission> findByProblemId(Long problemId, Pageable pageable);

    Page<Submission> findByVerdict(Verdict verdict, Pageable pageable);

    Page<Submission> findByUserIdAndVerdict(Long userId, Verdict verdict, Pageable pageable);

    Page<Submission> findByProblemIdAndVerdict(Long problemId, Verdict verdict, Pageable pageable);

    Page<Submission> findByUserIdAndProblemIdAndVerdict(Long userId, Long problemId, Verdict verdict, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime timestamp);

    long countByVerdict(Verdict verdict);

    long countByProblemId(Long problemId);
}
