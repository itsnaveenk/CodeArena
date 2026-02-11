package com.codearena.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.ContestLeaderboardEntry;

@Repository
public interface ContestLeaderboardEntryRepository extends JpaRepository<ContestLeaderboardEntry, Long> {

    Optional<ContestLeaderboardEntry> findByContestIdAndUserId(Long contestId, Long userId);

    @Query("SELECT e FROM ContestLeaderboardEntry e WHERE e.contest.id = :contestId " +
           "ORDER BY e.totalPoints DESC, e.totalTimeSeconds ASC")
    Page<ContestLeaderboardEntry> findByContestIdOrderByRanking(@Param("contestId") Long contestId, Pageable pageable);

    @Query("SELECT e FROM ContestLeaderboardEntry e WHERE e.contest.id = :contestId " +
           "ORDER BY e.totalPoints DESC, e.totalTimeSeconds ASC")
    List<ContestLeaderboardEntry> findAllByContestIdOrderByRanking(@Param("contestId") Long contestId);

    @Query("SELECT COUNT(e) FROM ContestLeaderboardEntry e WHERE e.contest.id = :contestId")
    int countByContestId(@Param("contestId") Long contestId);
}
