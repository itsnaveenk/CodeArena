package com.codearena.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestStatus;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    Optional<Contest> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT c FROM Contest c WHERE c.status = :status")
    Page<Contest> findByStatus(@Param("status") ContestStatus status, Pageable pageable);

    @Query("SELECT c FROM Contest c WHERE c.visibility = 'PUBLIC' " +
           "AND c.status IN ('PUBLISHED', 'RUNNING', 'FINISHED') " +
           "ORDER BY c.startTime DESC")
    Page<Contest> findPublicContests(Pageable pageable);

    @Query("SELECT c FROM Contest c WHERE c.status = 'PUBLISHED' AND c.startTime <= :now")
    List<Contest> findContestsToStart(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Contest c WHERE c.status = 'RUNNING' AND c.endTime <= :now")
    List<Contest> findContestsToFinish(@Param("now") LocalDateTime now);

    Page<Contest> findByCreatedById(Long userId, Pageable pageable);
}
