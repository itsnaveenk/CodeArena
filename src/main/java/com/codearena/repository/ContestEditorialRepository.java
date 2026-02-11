package com.codearena.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codearena.entity.ContestEditorial;

@Repository
public interface ContestEditorialRepository extends JpaRepository<ContestEditorial, Long> {

    Optional<ContestEditorial> findByContestIdAndProblemId(Long contestId, Long problemId);

    List<ContestEditorial> findByContestId(Long contestId);
}
