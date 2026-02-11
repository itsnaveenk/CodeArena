package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.ContestInvitation;
import com.codearena.entity.InvitationStatus;

public record ContestInvitationDto(
    Long id,
    Long contestId,
    String invitedEmail,
    Long invitedUserId,
    String invitedUserName,
    InvitationStatus status,
    LocalDateTime invitedAt,
    String invitedByName,
    LocalDateTime acceptedAt,
    LocalDateTime declinedAt,
    LocalDateTime revokedAt
) {
    public static ContestInvitationDto fromEntity(ContestInvitation invitation) {
        if (invitation == null) {
            return null;
        }
        return new ContestInvitationDto(
            invitation.getId(),
            invitation.getContest().getId(),
            invitation.getInvitedEmail(),
            invitation.getInvitedUser() != null ? invitation.getInvitedUser().getId() : null,
            invitation.getInvitedUser() != null ? invitation.getInvitedUser().getName() : null,
            invitation.getStatus(),
            invitation.getInvitedAt(),
            invitation.getInvitedBy() != null ? invitation.getInvitedBy().getName() : null,
            invitation.getAcceptedAt(),
            invitation.getDeclinedAt(),
            invitation.getRevokedAt()
        );
    }

    public static ContestInvitationDto of(Long id, Long contestId, String invitedEmail,
                                           Long invitedUserId, String invitedUserName,
                                           InvitationStatus status, LocalDateTime invitedAt,
                                           String invitedByName, LocalDateTime acceptedAt,
                                           LocalDateTime declinedAt, LocalDateTime revokedAt) {
        return new ContestInvitationDto(id, contestId, invitedEmail, invitedUserId, invitedUserName,
                                         status, invitedAt, invitedByName, acceptedAt, declinedAt, revokedAt);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isDeclined() {
        return status == InvitationStatus.DECLINED;
    }

    public boolean isRevoked() {
        return status == InvitationStatus.REVOKED;
    }

    public boolean isExpired() {
        return status == InvitationStatus.EXPIRED;
    }

    public boolean hasExistingUser() {
        return invitedUserId != null;
    }

    public boolean canBeRevoked() {
        return status == InvitationStatus.PENDING;
    }
}
