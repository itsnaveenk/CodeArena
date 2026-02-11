package com.codearena.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.codearena.document.ContestDraft;

@Repository
public interface ContestDraftRepository extends MongoRepository<ContestDraft, String> {

    Optional<ContestDraft> findByContestIdAndProblemIdAndUserId(Long contestId, Long problemId, Long userId);

    List<ContestDraft> findByContestIdAndUserId(Long contestId, Long userId);

    long deleteByContestIdAndSavedAtBefore(Long contestId, LocalDateTime before);

    long deleteByContestId(Long contestId);
}
