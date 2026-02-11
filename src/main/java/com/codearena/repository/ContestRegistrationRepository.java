package com.codearena.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.ContestRegistration;
import com.codearena.entity.RegistrationStatus;

@Repository
public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration, Long> {

    Optional<ContestRegistration> findByContestIdAndUserId(Long contestId, Long userId);

    boolean existsByContestIdAndUserIdAndStatus(Long contestId, Long userId, RegistrationStatus status);

    @Query("SELECT COUNT(r) FROM ContestRegistration r " +
           "WHERE r.contest.id = :contestId AND r.status = 'REGISTERED'")
    int countRegisteredByContestId(@Param("contestId") Long contestId);

    Page<ContestRegistration> findByContestIdAndStatus(Long contestId, RegistrationStatus status, Pageable pageable);

    @Query("SELECT r FROM ContestRegistration r WHERE r.user.id = :userId AND r.status = 'REGISTERED'")
    Page<ContestRegistration> findByUserIdAndRegistered(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM ContestRegistration r WHERE r.user.id = :userId")
    Page<ContestRegistration> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
