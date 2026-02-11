package com.codearena.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codearena.entity.StarterCode;

@Repository
public interface StarterCodeRepository extends JpaRepository<StarterCode, Long> {

    List<StarterCode> findByProblemId(Long problemId);

    Optional<StarterCode> findByProblemIdAndLanguageId(Long problemId, Integer languageId);

    boolean existsByProblemIdAndLanguageId(Long problemId, Integer languageId);

    void deleteByProblemId(Long problemId);

    long countByProblemId(Long problemId);
}
