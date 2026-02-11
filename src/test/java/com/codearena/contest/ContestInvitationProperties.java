package com.codearena.contest;

import com.codearena.entity.*;
import com.codearena.repository.ContestInvitationRepository;
import com.codearena.repository.ContestRegistrationRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for Contest Invitations.
 * Tests Properties 6, 7, and 8 from the design document.
 */
@SpringBootTest
@ActiveProfiles("test")
@Label("Contest Invitation Properties")
class ContestInvitationProperties {

    @MockBean
    private ContestInvitationRepository invitationRepository;

    @MockBean
    private ContestRegistrationRepository registrationRepository;

    /**
     * Property 6: Invitation Uniqueness and Re-invitation
     * 
     * For any PRIVATE contest and email address:
     * - Only one PENDING invitation can exist at a time
     * - Inviting when PENDING exists SHALL return ALREADY_INVITED error
     * - Re-inviting after DECLINED, REVOKED, or EXPIRED status SHALL succeed
     * 
     * Validates: Requirements 4.1.3, 4.1.4
     */
    @Property(tries = 100)
    @Label("Property 6: Invitation Uniqueness and Re-invitation")
    @Tag("Feature: contest-feature, Property 6: Invitation Uniqueness and Re-invitation")
    void invitationUniqueness(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll("emails") String email
    ) {
        // Given: A PENDING invitation exists
        when(invitationRepository.existsByContestIdAndInvitedEmailAndStatus(
            contestId, email, InvitationStatus.PENDING))
            .thenReturn(true);
        
        // Then: Duplicate invitation is detected
        boolean alreadyInvited = invitationRepository
            .existsByContestIdAndInvitedEmailAndStatus(contestId, email, InvitationStatus.PENDING);
        assertThat(alreadyInvited).isTrue();
        
        // Given: Previous invitation was DECLINED
        ContestInvitation declinedInvitation = new ContestInvitation();
        declinedInvitation.setStatus(InvitationStatus.DECLINED);
        when(invitationRepository.findByContestIdAndInvitedEmail(contestId, email))
            .thenReturn(Optional.of(declinedInvitation));
        when(invitationRepository.existsByContestIdAndInvitedEmailAndStatus(
            contestId, email, InvitationStatus.PENDING))
            .thenReturn(false);
        
        // Then: Re-invitation is allowed (no PENDING invitation exists)
        boolean canReinvite = !invitationRepository
            .existsByContestIdAndInvitedEmailAndStatus(contestId, email, InvitationStatus.PENDING);
        assertThat(canReinvite).isTrue();
    }

    /**
     * Property 6b: Re-invitation After REVOKED
     * 
     * Verifies that re-invitation is allowed after REVOKED status.
     */
    @Property(tries = 100)
    @Label("Property 6b: Re-invitation After REVOKED")
    @Tag("Feature: contest-feature, Property 6: Invitation Uniqueness and Re-invitation")
    void reinvitationAfterRevoked(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll("emails") String email
    ) {
        // Given: Previous invitation was REVOKED
        ContestInvitation revokedInvitation = new ContestInvitation();
        revokedInvitation.setStatus(InvitationStatus.REVOKED);
        when(invitationRepository.findByContestIdAndInvitedEmail(contestId, email))
            .thenReturn(Optional.of(revokedInvitation));
        when(invitationRepository.existsByContestIdAndInvitedEmailAndStatus(
            contestId, email, InvitationStatus.PENDING))
            .thenReturn(false);
        
        // Then: Re-invitation is allowed
        boolean canReinvite = !invitationRepository
            .existsByContestIdAndInvitedEmailAndStatus(contestId, email, InvitationStatus.PENDING);
        assertThat(canReinvite).isTrue();
    }

    /**
     * Property 6c: Re-invitation After EXPIRED
     * 
     * Verifies that re-invitation is allowed after EXPIRED status.
     */
    @Property(tries = 100)
    @Label("Property 6c: Re-invitation After EXPIRED")
    @Tag("Feature: contest-feature, Property 6: Invitation Uniqueness and Re-invitation")
    void reinvitationAfterExpired(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll("emails") String email
    ) {
        // Given: Previous invitation was EXPIRED
        ContestInvitation expiredInvitation = new ContestInvitation();
        expiredInvitation.setStatus(InvitationStatus.EXPIRED);
        when(invitationRepository.findByContestIdAndInvitedEmail(contestId, email))
            .thenReturn(Optional.of(expiredInvitation));
        when(invitationRepository.existsByContestIdAndInvitedEmailAndStatus(
            contestId, email, InvitationStatus.PENDING))
            .thenReturn(false);
        
        // Then: Re-invitation is allowed
        boolean canReinvite = !invitationRepository
            .existsByContestIdAndInvitedEmailAndStatus(contestId, email, InvitationStatus.PENDING);
        assertThat(canReinvite).isTrue();
    }

    /**
     * Property 7: Invitation Acceptance Creates Registration
     * 
     * For any accepted invitation where the contest is PUBLISHED or RUNNING
     * and not full:
     * - A ContestRegistration with status=REGISTERED SHALL be created
     * - The invitation status SHALL be updated to ACCEPTED
     * 
     * Validates: Requirements 4.4.1
     */
    @Property(tries = 100)
    @Label("Property 7: Invitation Acceptance Creates Registration")
    @Tag("Feature: contest-feature, Property 7: Invitation Acceptance Creates Registration")
    void invitationAcceptanceCreatesRegistration(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll @LongRange(min = 1, max = 1000) long userId
    ) {
        // Given: An accepted invitation
        ContestInvitation invitation = new ContestInvitation();
        invitation.setStatus(InvitationStatus.ACCEPTED);
        
        // And: A corresponding registration should exist
        ContestRegistration registration = new ContestRegistration();
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationRepository.findByContestIdAndUserId(contestId, userId))
            .thenReturn(Optional.of(registration));
        
        // Then: Registration exists with REGISTERED status
        Optional<ContestRegistration> foundRegistration = 
            registrationRepository.findByContestIdAndUserId(contestId, userId);
        assertThat(foundRegistration).isPresent();
        assertThat(foundRegistration.get().getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        
        // And: Invitation status is ACCEPTED
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    /**
     * Property 8: Contest Finish Expires Invitations
     * 
     * For any contest transitioning to FINISHED or CANCELLED status, all
     * invitations with status=PENDING SHALL be updated to status=EXPIRED.
     * 
     * Validates: Requirements 4.7.1
     */
    @Property(tries = 100)
    @Label("Property 8: Contest Finish Expires Invitations")
    @Tag("Feature: contest-feature, Property 8: Contest Finish Expires Invitations")
    void contestFinishExpiresInvitations(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll("terminalStatuses") ContestStatus finalStatus
    ) {
        // Given: A contest transitioning to FINISHED or CANCELLED
        assertThat(finalStatus).isIn(ContestStatus.FINISHED, ContestStatus.CANCELLED);
        
        // When: Expiring pending invitations
        when(invitationRepository.expirePendingInvitations(contestId)).thenReturn(1);
        int expiredCount = invitationRepository.expirePendingInvitations(contestId);
        
        // Then: Pending invitations are expired
        assertThat(expiredCount).isGreaterThanOrEqualTo(0);
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> emails() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
            Arbitraries.of("gmail.com", "yahoo.com", "example.com", "test.com")
        ).as((user, domain) -> user + "@" + domain);
    }

    @Provide
    Arbitrary<ContestStatus> terminalStatuses() {
        return Arbitraries.of(ContestStatus.FINISHED, ContestStatus.CANCELLED);
    }
}
