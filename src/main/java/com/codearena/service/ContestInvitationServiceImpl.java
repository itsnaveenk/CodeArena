package com.codearena.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.BulkInviteResult;
import com.codearena.dto.ContestInvitationDto;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestInvitation;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.InvitationStatus;
import com.codearena.entity.RegistrationStatus;
import com.codearena.entity.User;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.ContestRegistrationException;
import com.codearena.exception.ContestValidationException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestInvitationRepository;
import com.codearena.repository.ContestRegistrationRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.UserRepository;

@Service
@Transactional
public class ContestInvitationServiceImpl implements ContestInvitationService {

    private static final Logger log = LoggerFactory.getLogger(ContestInvitationServiceImpl.class);

    private static final int MAX_BULK_INVITE_SIZE = 100;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final ContestRepository contestRepository;
    private final ContestInvitationRepository invitationRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public ContestInvitationServiceImpl(ContestRepository contestRepository,
                                        ContestInvitationRepository invitationRepository,
                                        ContestRegistrationRepository registrationRepository,
                                        UserRepository userRepository) {
        this.contestRepository = contestRepository;
        this.invitationRepository = invitationRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }


    @Override
    public ContestInvitation invite(Long contestId, String email, User inviter) {
        log.debug("Inviting {} to contest {}", email, contestId);

        if (!isValidEmail(email)) {
            throw new ContestValidationException("Invalid email format: " + email);
        }

        Contest contest = findContestOrThrow(contestId);

        if (contest.getVisibility() != ContestVisibility.PRIVATE) {
            throw new ContestValidationException("Invitations are only allowed for PRIVATE contests");
        }

        String normalizedEmail = email.toLowerCase().trim();

        var existingInvitation = invitationRepository.findByContestIdAndInvitedEmail(contestId, normalizedEmail);

        if (existingInvitation.isPresent()) {
            ContestInvitation invitation = existingInvitation.get();

            if (invitation.getStatus() == InvitationStatus.PENDING) {
                throw ContestRegistrationException.registrationNotOpen("Already invited");
            }

            if (invitation.getStatus() == InvitationStatus.DECLINED ||
                invitation.getStatus() == InvitationStatus.REVOKED ||
                invitation.getStatus() == InvitationStatus.EXPIRED) {
                
                log.debug("Re-inviting {} to contest {} (previous status: {})", 
                    normalizedEmail, contestId, invitation.getStatus());
                
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setInvitationToken(UUID.randomUUID().toString());
                invitation.setInvitedBy(inviter);
                invitation.setInvitedAt(LocalDateTime.now());
                invitation.setAcceptedAt(null);
                invitation.setDeclinedAt(null);
                invitation.setRevokedAt(null);

                User invitedUser = userRepository.findByEmail(normalizedEmail).orElse(null);
                invitation.setInvitedUser(invitedUser);

                log.info("Re-invited {} to contest {}", normalizedEmail, contestId);
                return invitationRepository.save(invitation);
            }

            throw ContestRegistrationException.alreadyRegistered();
        }

        ContestInvitation invitation = new ContestInvitation(contest, normalizedEmail, inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitationToken(UUID.randomUUID().toString());
        invitation.setInvitedAt(LocalDateTime.now());

        User invitedUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        invitation.setInvitedUser(invitedUser);

        log.info("Invited {} to contest {}", normalizedEmail, contestId);
        return invitationRepository.save(invitation);
    }

    @Override
    public List<BulkInviteResult> bulkInvite(Long contestId, List<String> emails, User inviter) {
        log.debug("Bulk inviting {} emails to contest {}", emails.size(), contestId);

        if (emails.size() > MAX_BULK_INVITE_SIZE) {
            throw new ContestValidationException(
                "Maximum " + MAX_BULK_INVITE_SIZE + " emails allowed per bulk invite request");
        }

        Contest contest = findContestOrThrow(contestId);

        if (contest.getVisibility() != ContestVisibility.PRIVATE) {
            throw new ContestValidationException("Invitations are only allowed for PRIVATE contests");
        }

        List<BulkInviteResult> results = new ArrayList<>();

        for (String email : emails) {
            BulkInviteResult result = processEmailInvitation(contestId, email, inviter);
            results.add(result);
        }

        log.info("Bulk invite to contest {} completed: {} emails processed", contestId, results.size());
        return results;
    }


    @Override
    public void acceptInvitation(String token, User user) {
        log.debug("Accepting invitation with token {} for user {}", token, user.getId());

        ContestInvitation invitation = findInvitationByTokenOrThrow(token);
        Contest contest = invitation.getContest();

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ContestRegistrationException.registrationNotOpen(
                "Invitation is no longer pending (status: " + invitation.getStatus() + ")");
        }

        if (contest.getStatus() != ContestStatus.PUBLISHED && contest.getStatus() != ContestStatus.RUNNING) {
            throw ContestRegistrationException.registrationNotOpen(
                "Contest is not accepting registrations (status: " + contest.getStatus() + ")");
        }

        if (contest.getMaxParticipants() != null) {
            int currentCount = registrationRepository.countRegisteredByContestId(contest.getId());
            if (currentCount >= contest.getMaxParticipants()) {
                throw ContestRegistrationException.contestFull();
            }
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setInvitedUser(user);
        invitationRepository.save(invitation);

        var existingRegistration = registrationRepository.findByContestIdAndUserId(contest.getId(), user.getId());

        if (existingRegistration.isPresent()) {
            ContestRegistration registration = existingRegistration.get();
            if (registration.getStatus() == RegistrationStatus.WITHDRAWN) {
                registration.setStatus(RegistrationStatus.REGISTERED);
                registration.setRegisteredAt(LocalDateTime.now());
                registration.setWithdrawnAt(null);
                registrationRepository.save(registration);
            }
        } else {
            ContestRegistration registration = new ContestRegistration(contest, user);
            registration.setStatus(RegistrationStatus.REGISTERED);
            registration.setRegisteredAt(LocalDateTime.now());
            registrationRepository.save(registration);
        }

        log.info("User {} accepted invitation to contest {}", user.getId(), contest.getId());
    }

    @Override
    public void declineInvitation(String token, User user) {
        log.debug("Declining invitation with token {} for user {}", token, user.getId());

        ContestInvitation invitation = findInvitationByTokenOrThrow(token);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ContestRegistrationException.registrationNotOpen(
                "Invitation is no longer pending (status: " + invitation.getStatus() + ")");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setDeclinedAt(LocalDateTime.now());
        invitation.setInvitedUser(user);
        invitationRepository.save(invitation);

        log.info("User {} declined invitation to contest {}", user.getId(), invitation.getContest().getId());
    }

    @Override
    public void revokeInvitation(Long invitationId, User revoker) {
        log.debug("Revoking invitation {} by user {}", invitationId, revoker.getId());

        ContestInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));

        Contest contest = invitation.getContest();

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ContestRegistrationException.registrationNotOpen(
                "Can only revoke PENDING invitations (status: " + invitation.getStatus() + ")");
        }

        if (contest.getStatus() == ContestStatus.RUNNING) {
            throw ContestRegistrationException.registrationNotOpen(
                "Cannot revoke invitations while contest is running");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        log.info("Invitation {} revoked by user {}", invitationId, revoker.getId());
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ContestInvitationDto> getContestInvitations(Long contestId, Pageable pageable) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return invitationRepository.findByContestId(contestId, pageable)
            .map(ContestInvitationDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ContestInvitation getInvitationByToken(String token) {
        return invitationRepository.findByInvitationToken(token).orElse(null);
    }


    private Contest findContestOrThrow(Long contestId) {
        return contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));
    }

    private ContestInvitation findInvitationByTokenOrThrow(String token) {
        return invitationRepository.findByInvitationToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found for token: " + token));
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private BulkInviteResult processEmailInvitation(Long contestId, String email, User inviter) {
        if (!isValidEmail(email)) {
            return BulkInviteResult.invalidEmail(email);
        }

        String normalizedEmail = email.toLowerCase().trim();

        var existingInvitation = invitationRepository.findByContestIdAndInvitedEmail(contestId, normalizedEmail);

        if (existingInvitation.isPresent()) {
            ContestInvitation invitation = existingInvitation.get();

            if (invitation.getStatus() == InvitationStatus.PENDING) {
                return BulkInviteResult.alreadyInvited(email);
            }

            if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
                return BulkInviteResult.alreadyInvited(email);
            }

            if (invitation.getStatus() == InvitationStatus.DECLINED ||
                invitation.getStatus() == InvitationStatus.REVOKED ||
                invitation.getStatus() == InvitationStatus.EXPIRED) {
                
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setInvitationToken(UUID.randomUUID().toString());
                invitation.setInvitedBy(inviter);
                invitation.setInvitedAt(LocalDateTime.now());
                invitation.setAcceptedAt(null);
                invitation.setDeclinedAt(null);
                invitation.setRevokedAt(null);

                User invitedUser = userRepository.findByEmail(normalizedEmail).orElse(null);
                invitation.setInvitedUser(invitedUser);

                invitationRepository.save(invitation);
                return BulkInviteResult.invited(email);
            }
        }

        Contest contest = contestRepository.getReferenceById(contestId);
        ContestInvitation invitation = new ContestInvitation(contest, normalizedEmail, inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitationToken(UUID.randomUUID().toString());
        invitation.setInvitedAt(LocalDateTime.now());

        User invitedUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        invitation.setInvitedUser(invitedUser);

        invitationRepository.save(invitation);
        return BulkInviteResult.invited(email);
    }
}
