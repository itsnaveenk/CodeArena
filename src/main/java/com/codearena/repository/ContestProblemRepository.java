package com.codearena.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.ContestProblem;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {

    List<ContestProblem> findByContestIdOrderByDisplayOrderAsc(Long contestId);

    Optional<ContestProblem> findByContestIdAndProblemId(Long contestId, Long problemId);

    boolean existsByContestIdAndProblemId(Long contestId, Long problemId);

    @Query("SELECT MAX(cp.displayOrder) FROM ContestProblem cp WHERE cp.contest.id = :contestId")
    Optional<Integer> findMaxDisplayOrder(@Param("contestId") Long contestId);

    @Query("SELECT COALESCE(SUM(cp.pointValue), 0) FROM ContestProblem cp WHERE cp.contest.id = :contestId")
    Integer sumPointValueByContestId(@Param("contestId") Long contestId);

    void deleteByContestIdAndProblemId(Long contestId, Long problemId);

    int countByContestId(Long contestId);
}
