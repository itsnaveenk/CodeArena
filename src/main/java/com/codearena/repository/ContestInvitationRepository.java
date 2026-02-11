package com.codearena.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.ContestInvitation;
import com.codearena.entity.InvitationStatus;

@Repository
public interface ContestInvitationRepository extends JpaRepository<ContestInvitation, Long> {

    Optional<ContestInvitation> findByInvitationToken(String token);

    Optional<ContestInvitation> findByContestIdAndInvitedEmail(Long contestId, String email);

    boolean existsByContestIdAndInvitedEmailAndStatus(Long contestId, String email, InvitationStatus status);

    Page<ContestInvitation> findByContestId(Long contestId, Pageable pageable);

    @Modifying
    @Query("UPDATE ContestInvitation i SET i.status = 'EXPIRED' " +
           "WHERE i.contest.id = :contestId AND i.status = 'PENDING'")
    int expirePendingInvitations(@Param("contestId") Long contestId);

    @Query("SELECT i FROM ContestInvitation i WHERE i.invitedUser.id = :userId")
    Page<ContestInvitation> findByInvitedUserId(@Param("userId") Long userId, Pageable pageable);
}
