package com.codearena.repository;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.codearena.document.ContestRunAttempt;

@Repository
public interface ContestRunAttemptRepository extends MongoRepository<ContestRunAttempt, String> {

    long countByContestIdAndUserIdAndProblemIdAndCreatedAtAfter(
        Long contestId, Long userId, Long problemId, LocalDateTime after);

    long countByContestIdAndUserIdAndCreatedAtAfter(
        Long contestId, Long userId, LocalDateTime after);
}
