package com.codearena.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestStatus;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.InvalidStatusTransitionException;
import com.codearena.repository.ContestInvitationRepository;
import com.codearena.repository.ContestRepository;

@Service
@Transactional(readOnly = true)
public class ContestLifecycleServiceImpl implements ContestLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ContestLifecycleServiceImpl.class);

    private final ContestRepository contestRepository;
    private final ContestInvitationRepository contestInvitationRepository;
    private final ContestLeaderboardService contestLeaderboardService;
    private final ContestSubmissionService contestSubmissionService;

    public ContestLifecycleServiceImpl(ContestRepository contestRepository,
                                       ContestInvitationRepository contestInvitationRepository,
                                       ContestLeaderboardService contestLeaderboardService,
                                       ContestSubmissionService contestSubmissionService) {
        this.contestRepository = contestRepository;
        this.contestInvitationRepository = contestInvitationRepository;
        this.contestLeaderboardService = contestLeaderboardService;
        this.contestSubmissionService = contestSubmissionService;
    }

    @Override
    @Transactional
    public void processScheduledTransitions() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Processing scheduled contest transitions at {}", now);

        List<Contest> contestsToStart = contestRepository.findContestsToStart(now);
        for (Contest contest : contestsToStart) {
            try {
                log.info("Transitioning contest {} ({}) from PUBLISHED to RUNNING", 
                    contest.getId(), contest.getTitle());
                transitionToRunning(contest.getId());
            } catch (Exception e) {
                log.error("Failed to transition contest {} to RUNNING: {}", 
                    contest.getId(), e.getMessage(), e);
            }
        }

        List<Contest> contestsToFinish = contestRepository.findContestsToFinish(now);
        for (Contest contest : contestsToFinish) {
            try {
                log.info("Transitioning contest {} ({}) from RUNNING to FINISHED", 
                    contest.getId(), contest.getTitle());
                transitionToFinished(contest.getId());
            } catch (Exception e) {
                log.error("Failed to transition contest {} to FINISHED: {}", 
                    contest.getId(), e.getMessage(), e);
            }
        }

        if (!contestsToStart.isEmpty() || !contestsToFinish.isEmpty()) {
            log.info("Processed {} contests to start, {} contests to finish", 
                contestsToStart.size(), contestsToFinish.size());
        }
    }

    @Override
    @Transactional
    public void transitionToRunning(Long contestId) {
        log.debug("Transitioning contest {} to RUNNING", contestId);

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        if (contest.getStatus() != ContestStatus.PUBLISHED) {
            throw new InvalidStatusTransitionException(contest.getStatus(), ContestStatus.RUNNING);
        }

        contest.setStatus(ContestStatus.RUNNING);
        contestRepository.save(contest);

        log.info("Contest {} ({}) transitioned to RUNNING", contestId, contest.getTitle());
    }

    @Override
    @Transactional
    public void transitionToFinished(Long contestId) {
        log.debug("Transitioning contest {} to FINISHED", contestId);

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        if (contest.getStatus() != ContestStatus.RUNNING) {
            throw new InvalidStatusTransitionException(contest.getStatus(), ContestStatus.FINISHED);
        }

        contest.setStatus(ContestStatus.FINISHED);
        contestRepository.save(contest);

        log.info("Contest {} ({}) transitioned to FINISHED", contestId, contest.getTitle());

        finalizeContest(contestId);
    }

    @Override
    @Transactional
    public void finalizeContest(Long contestId) {
        log.debug("Finalizing contest {}", contestId);

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        log.debug("Auto-submitting drafts for contest {}", contestId);
        try {
            contestSubmissionService.autoSubmitForContest(contestId);
        } catch (Exception e) {
            log.error("Auto-submit failed for contest {}: {}", contestId, e.getMessage(), e);
        }

        log.debug("Recalculating final rankings for contest {}", contestId);
        contestLeaderboardService.recalculateAllRankings(contestId);

        log.debug("Assigning medals for contest {}", contestId);
        contestLeaderboardService.assignMedals(contestId);

        log.debug("Expiring pending invitations for contest {}", contestId);
        int expiredCount = contestInvitationRepository.expirePendingInvitations(contestId);
        if (expiredCount > 0) {
            log.info("Expired {} pending invitations for contest {}", expiredCount, contestId);
        }

        log.info("Contest {} ({}) finalized successfully", contestId, contest.getTitle());
    }
}
