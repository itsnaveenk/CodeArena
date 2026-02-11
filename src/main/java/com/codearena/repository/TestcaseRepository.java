package com.codearena.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codearena.entity.Testcase;

@Repository
public interface TestcaseRepository extends JpaRepository<Testcase, Long> {

    List<Testcase> findByProblemId(Long problemId);

    List<Testcase> findByProblemIdAndIsHidden(Long problemId, boolean isHidden);

    long countByProblemId(Long problemId);

    long countByProblemIdAndIsHidden(Long problemId, boolean isHidden);

    void deleteByProblemId(Long problemId);
}
