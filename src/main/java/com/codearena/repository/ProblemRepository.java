package com.codearena.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Problem> findByStatus(ProblemStatus status, Pageable pageable);

    Page<Problem> findByStatusAndDifficulty(ProblemStatus status, Difficulty difficulty, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = 'PUBLISHED'")
    Page<Problem> findAllPublished(Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = 'PUBLISHED' AND p.difficulty = :difficulty")
    Page<Problem> findPublishedByDifficulty(@Param("difficulty") Difficulty difficulty, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = 'PUBLISHED' AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Problem> findPublishedByTitleContaining(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = 'PUBLISHED' AND p.difficulty = :difficulty AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Problem> findPublishedByDifficultyAndTitleContaining(
            @Param("difficulty") Difficulty difficulty,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.createdBy.id = :userId")
    Page<Problem> findByCreatedById(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = 'PENDING_REVIEW'")
    Page<Problem> findAllPendingReview(Pageable pageable);

    long countByStatus(ProblemStatus status);

    @Query("SELECT COUNT(p) FROM Problem p WHERE p.status = 'PUBLISHED' AND p.difficulty = :difficulty")
    long countPublishedByDifficulty(@Param("difficulty") Difficulty difficulty);

    List<Problem> findByIdIn(List<Long> ids);

    @Query(value = "SELECT * FROM problems p WHERE p.status = 'PUBLISHED' AND JSON_CONTAINS(p.tags, :tag, '$')", 
           countQuery = "SELECT COUNT(*) FROM problems p WHERE p.status = 'PUBLISHED' AND JSON_CONTAINS(p.tags, :tag, '$')",
           nativeQuery = true)
    Page<Problem> findPublishedByTag(@Param("tag") String tag, Pageable pageable);

    long countByDifficulty(Difficulty difficulty);

    @Query("SELECT p FROM Problem p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Problem> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = :status AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.createdBy.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Problem> findByStatusAndTitleOrAuthorContaining(@Param("status") ProblemStatus status,
                                                          @Param("keyword") String keyword,
                                                          Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.difficulty = :difficulty AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.createdBy.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Problem> findByDifficultyAndTitleOrAuthorContaining(@Param("difficulty") Difficulty difficulty,
                                                              @Param("keyword") String keyword,
                                                              Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.status = :status AND p.difficulty = :difficulty AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.createdBy.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Problem> findByStatusAndDifficultyAndTitleOrAuthorContaining(
            @Param("status") ProblemStatus status,
            @Param("difficulty") Difficulty difficulty,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.createdBy.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Problem> findByTitleOrAuthorContaining(@Param("keyword") String keyword, Pageable pageable);
}
