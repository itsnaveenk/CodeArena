package com.codearena.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.ContestRegistrationDto;
import com.codearena.dto.ParticipationStatus;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.RegistrationStatus;
import com.codearena.entity.TimerMode;
import com.codearena.entity.User;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.ContestRegistrationException;
import com.codearena.repository.ContestRegistrationRepository;
import com.codearena.repository.ContestRepository;

@Service
@Transactional
public class ContestRegistrationServiceImpl implements ContestRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ContestRegistrationServiceImpl.class);

    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository registrationRepository;

    public ContestRegistrationServiceImpl(ContestRepository contestRepository,
                                          ContestRegistrationRepository registrationRepository) {
        this.contestRepository = contestRepository;
        this.registrationRepository = registrationRepository;
    }


    @Override
    public ContestRegistration register(Long contestId, User user) {
        log.debug("Registering user {} for contest {}", user.getId(), contestId);

        Contest contest = findContestOrThrow(contestId);

        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            throw ContestRegistrationException.privateContest();
        }

        if (contest.getStatus() != ContestStatus.PUBLISHED) {
            throw ContestRegistrationException.registrationNotOpen(
                "Contest is not accepting registrations (status: " + contest.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(contest.getRegistrationStartTime())) {
            throw ContestRegistrationException.registrationNotOpen("Registration not open yet");
        }
        if (now.isAfter(contest.getRegistrationEndTime())) {
            throw ContestRegistrationException.registrationNotOpen("Registration closed");
        }

        var existingRegistration = registrationRepository.findByContestIdAndUserId(contestId, user.getId());

        if (existingRegistration.isPresent()) {
            ContestRegistration registration = existingRegistration.get();

            if (registration.getStatus() == RegistrationStatus.WITHDRAWN) {
                log.debug("Re-registering previously withdrawn user {} for contest {}", user.getId(), contestId);
                registration.setStatus(RegistrationStatus.REGISTERED);
                registration.setRegisteredAt(now);
                registration.setWithdrawnAt(null);
                return registrationRepository.save(registration);
            }

            throw ContestRegistrationException.alreadyRegistered();
        }

        if (contest.getMaxParticipants() != null) {
            int currentCount = registrationRepository.countRegisteredByContestId(contestId);
            if (currentCount >= contest.getMaxParticipants()) {
                throw ContestRegistrationException.contestFull();
            }
        }

        ContestRegistration registration = new ContestRegistration(contest, user);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(now);

        log.info("User {} registered for contest {}", user.getId(), contestId);
        return registrationRepository.save(registration);
    }

    @Override
    public void withdraw(Long contestId, User user) {
        log.debug("Withdrawing user {} from contest {}", user.getId(), contestId);

        Contest contest = findContestOrThrow(contestId);

        if (contest.getStatus() != ContestStatus.PUBLISHED) {
            throw ContestRegistrationException.registrationNotOpen(
                "Cannot withdraw from contest (status: " + contest.getStatus() + ")");
        }

        ContestRegistration registration = registrationRepository
            .findByContestIdAndUserId(contestId, user.getId())
            .orElseThrow(ContestRegistrationException::notRegistered);

        if (registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw ContestRegistrationException.notRegistered();
        }

        registration.setStatus(RegistrationStatus.WITHDRAWN);
        registration.setWithdrawnAt(LocalDateTime.now());
        registrationRepository.save(registration);

        log.info("User {} withdrew from contest {}", user.getId(), contestId);
    }

    @Override
    public void startContest(Long contestId, User user) {
        log.debug("Starting contest {} for user {}", contestId, user.getId());

        Contest contest = findContestOrThrow(contestId);

        if (contest.getStatus() != ContestStatus.RUNNING) {
            throw ContestRegistrationException.cannotStart(
                "Contest is not currently running (status: " + contest.getStatus() + ")");
        }

        if (contest.getTimerMode() != TimerMode.INDIVIDUAL) {
            throw ContestRegistrationException.cannotStart(
                "This contest uses a global timer - no need to start individually");
        }

        ContestRegistration registration = registrationRepository
            .findByContestIdAndUserId(contestId, user.getId())
            .orElseThrow(ContestRegistrationException::notRegistered);

        if (registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw ContestRegistrationException.notRegistered();
        }

        if (registration.hasStarted()) {
            throw ContestRegistrationException.alreadyStarted();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestStartTime = contest.getEndTime().minusMinutes(contest.getDurationMinutes());
        
        if (now.isAfter(latestStartTime)) {
            throw ContestRegistrationException.cannotStart(
                "Contest window closing soon, not enough time remaining");
        }

        registration.setParticipantStartTime(now);
        registrationRepository.save(registration);

        log.info("User {} started contest {} at {}", user.getId(), contestId, now);
    }


    @Override
    @Transactional(readOnly = true)
    public ContestRegistrationDto getRegistration(Long contestId, User user) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return registrationRepository.findByContestIdAndUserId(contestId, user.getId())
            .map(ContestRegistrationDto::fromEntity)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContestRegistrationDto> getContestRegistrations(Long contestId, Pageable pageable) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return registrationRepository
            .findByContestIdAndStatus(contestId, RegistrationStatus.REGISTERED, pageable)
            .map(ContestRegistrationDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRegistered(Long contestId, Long userId) {
        return registrationRepository.existsByContestIdAndUserIdAndStatus(
            contestId, userId, RegistrationStatus.REGISTERED);
    }

    @Override
    @Transactional(readOnly = true)
    public int getRegisteredCount(Long contestId) {
        return registrationRepository.countRegisteredByContestId(contestId);
    }


    @Override
    @Transactional(readOnly = true)
    public ParticipationStatus checkParticipationStatus(Long contestId, User user) {
        Contest contest = findContestOrThrow(contestId);

        var registrationOpt = registrationRepository.findByContestIdAndUserId(contestId, user.getId());

        if (registrationOpt.isEmpty()) {
            return ParticipationStatus.notRegistered();
        }

        ContestRegistration registration = registrationOpt.get();

        if (registration.getStatus() != RegistrationStatus.REGISTERED) {
            return ParticipationStatus.withdrawn();
        }

        switch (contest.getStatus()) {
            case DRAFT:
            case PUBLISHED:
                return ParticipationStatus.contestNotStarted();
            case FINISHED:
            case CANCELLED:
                return ParticipationStatus.contestEnded();
            case RUNNING:
                break;
            default:
                return ParticipationStatus.contestNotRunning();
        }

        if (contest.getTimerMode() == TimerMode.INDIVIDUAL) {
            if (!registration.hasStarted()) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime latestStartTime = contest.getEndTime().minusMinutes(contest.getDurationMinutes());
                
                if (now.isAfter(latestStartTime)) {
                    return ParticipationStatus.notEnoughTime();
                }
                return ParticipationStatus.notStarted();
            }
        }

        long remainingSeconds = registration.getRemainingTimeSeconds();
        if (remainingSeconds <= 0) {
            return ParticipationStatus.timeExpired();
        }

        return ParticipationStatus.canParticipate(remainingSeconds);
    }


    private Contest findContestOrThrow(Long contestId) {
        return contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));
    }
}
