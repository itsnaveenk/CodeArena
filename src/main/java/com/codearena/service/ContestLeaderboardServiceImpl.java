package com.codearena.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.document.ContestSubmission;
import com.codearena.dto.ContestLeaderboardEntryDto;
import com.codearena.dto.ContestStatisticsDto;
import com.codearena.dto.ProblemScoreDto;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestLeaderboardEntry;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.MedalType;
import com.codearena.entity.ProblemScore;
import com.codearena.entity.Role;
import com.codearena.entity.TimerMode;
import com.codearena.entity.User;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.repository.ContestLeaderboardEntryRepository;
import com.codearena.repository.ContestRegistrationRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ContestSubmissionRepository;
import com.codearena.repository.UserRepository;

@Service
@Transactional
public class ContestLeaderboardServiceImpl implements ContestLeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(ContestLeaderboardServiceImpl.class);

    private final ContestRepository contestRepository;
    private final ContestLeaderboardEntryRepository leaderboardRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final ContestSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public ContestLeaderboardServiceImpl(
            ContestRepository contestRepository,
            ContestLeaderboardEntryRepository leaderboardRepository,
            ContestRegistrationRepository registrationRepository,
            ContestSubmissionRepository submissionRepository,
            UserRepository userRepository) {
        this.contestRepository = contestRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.registrationRepository = registrationRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "contestLeaderboards", key = "#contestId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ContestLeaderboardEntryDto> getLeaderboard(Long contestId, Pageable pageable, User currentUser) {
        Contest contest = findContestOrThrow(contestId);

        enforceLeaderboardAccess(contest, currentUser);

        Page<ContestLeaderboardEntry> entries = leaderboardRepository
                .findByContestIdOrderByRanking(contestId, pageable);

        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        return entries.map(entry -> toDto(entry, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public ContestLeaderboardEntryDto getUserRanking(Long contestId, User user) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return leaderboardRepository.findByContestIdAndUserId(contestId, user.getId())
                .map(entry -> toDto(entry, user.getId()))
                .orElse(null);
    }


    @Override
    @CacheEvict(value = "contestLeaderboards", allEntries = true)
    public void updateUserScore(Long contestId, Long userId, ContestSubmission submission) {
        log.debug("Updating score for userId={} in contestId={} after submission {}",
                userId, contestId, submission.getId());

        Contest contest = findContestOrThrow(contestId);

        ContestLeaderboardEntry entry = leaderboardRepository
                .findByContestIdAndUserId(contestId, userId)
                .orElseGet(() -> createNewEntry(contest, userId));

        ContestRegistration registration = registrationRepository
                .findByContestIdAndUserId(contestId, userId)
                .orElse(null);

        updateProblemScore(entry, submission, contest, registration);

        entry.recalculateTotals();
        entry.setLastSubmissionTime(submission.getSubmittedAt());

        leaderboardRepository.save(entry);

        recalculateAllRankings(contestId);

        log.info("Updated leaderboard entry for userId={}: totalPoints={}, rank={}",
                userId, entry.getTotalPoints(), entry.getRank());
    }

    @Override
    @CacheEvict(value = "contestLeaderboards", allEntries = true)
    public void recalculateAllRankings(Long contestId) {
        log.debug("Recalculating rankings for contestId={}", contestId);

        List<ContestLeaderboardEntry> entries = leaderboardRepository
                .findAllByContestIdOrderByRanking(contestId);

        if (entries.isEmpty()) {
            return;
        }

        int rank = 1;
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                ContestLeaderboardEntry prev = entries.get(i - 1);
                ContestLeaderboardEntry curr = entries.get(i);
                if (Double.compare(prev.getTotalPoints(), curr.getTotalPoints()) == 0
                        && prev.getTotalTimeSeconds().equals(curr.getTotalTimeSeconds())) {
                } else {
                    rank = i + 1; // Skip ranks for tied participants
                }
            }
            entries.get(i).setRank(rank);
        }

        leaderboardRepository.saveAll(entries);
    }

    @Override
    public void assignMedals(Long contestId) {
        log.debug("Assigning medals for contestId={}", contestId);

        List<ContestLeaderboardEntry> entries = leaderboardRepository
                .findAllByContestIdOrderByRanking(contestId);

        int totalParticipants = entries.size();
        if (totalParticipants == 0) {
            return;
        }

        int goldThreshold = (int) Math.ceil(totalParticipants * 0.10);
        int silverThreshold = (int) Math.ceil(totalParticipants * 0.25);
        int bronzeThreshold = (int) Math.ceil(totalParticipants * 0.50);

        for (ContestLeaderboardEntry entry : entries) {
            int rank = entry.getRank();
            if (rank <= goldThreshold) {
                entry.setMedal(MedalType.GOLD);
            } else if (rank <= silverThreshold) {
                entry.setMedal(MedalType.SILVER);
            } else if (rank <= bronzeThreshold) {
                entry.setMedal(MedalType.BRONZE);
            } else {
                entry.setMedal(null);
            }
        }

        leaderboardRepository.saveAll(entries);
        log.info("Assigned medals for contestId={}: {} participants", contestId, totalParticipants);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isLeaderboardFrozen(Long contestId) {
        Contest contest = findContestOrThrow(contestId);

        if (contest.getStatus() != ContestStatus.RUNNING) {
            return false;
        }

        if (contest.getLeaderboardFreezeMinutes() == null || contest.getLeaderboardFreezeMinutes() <= 0) {
            return false;
        }

        LocalDateTime freezeTime = contest.getEndTime().minusMinutes(contest.getLeaderboardFreezeMinutes());
        return LocalDateTime.now().isAfter(freezeTime);
    }


    @Override
    @Transactional(readOnly = true)
    public ContestStatisticsDto getStatistics(Long contestId) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        int totalParticipants = leaderboardRepository.countByContestId(contestId);
        long totalSubmissions = submissionRepository.countByContestId(contestId);

        List<ContestLeaderboardEntry> entries = leaderboardRepository
                .findAllByContestIdOrderByRanking(contestId);

        double averageScore = entries.stream()
                .mapToDouble(ContestLeaderboardEntry::getTotalPoints)
                .average()
                .orElse(0.0);

        return new ContestStatisticsDto(
                totalParticipants,
                totalSubmissions,
                averageScore,
                new java.util.HashMap<>());
    }


    private void enforceLeaderboardAccess(Contest contest, User currentUser) {
        if (contest.getVisibility() != ContestVisibility.PRIVATE) {
            return; // PUBLIC contests have no leaderboard restrictions
        }

        if (currentUser == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Leaderboard is not visible to participants in private contests");
        }

        boolean isCreator = contest.getCreatedBy() != null
                && contest.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Leaderboard is not visible to participants in private contests");
        }
    }


    private ContestLeaderboardEntry createNewEntry(Contest contest, Long userId) {
        ContestLeaderboardEntry entry = new ContestLeaderboardEntry();
        entry.setContest(contest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        entry.setUser(user);

        entry.setRank(0);
        entry.setTotalPoints(0.0);
        entry.setProblemsSolved(0);
        entry.setTotalTimeSeconds(0L);
        entry.setProblemScores(new ArrayList<>());
        entry.setLastUpdatedAt(LocalDateTime.now());

        return entry;
    }

    private void updateProblemScore(ContestLeaderboardEntry entry, ContestSubmission submission,
            Contest contest, ContestRegistration registration) {
        Long problemId = submission.getProblemId();
        Double newPoints = submission.getPointsEarned();

        Optional<ProblemScore> existingScore = entry.getProblemScores().stream()
                .filter(ps -> ps.getProblemId().equals(problemId))
                .findFirst();

        long solveTimeSeconds = calculateSolveTime(contest, registration, submission.getSubmittedAt());

        if (existingScore.isPresent()) {
            ProblemScore score = existingScore.get();
            score.incrementSubmissionCount();
            if (newPoints > score.getBestPoints()) {
                score.setBestPoints(newPoints);
                score.setSolveTimeSeconds(solveTimeSeconds);
                updateTotalTime(entry, contest, registration, submission.getSubmittedAt());
            }
        } else {
            ProblemScore newScore = new ProblemScore(problemId, newPoints, solveTimeSeconds, 1);
            entry.getProblemScores().add(newScore);
            if (newPoints > 0) {
                updateTotalTime(entry, contest, registration, submission.getSubmittedAt());
            }
        }
    }

    private long calculateSolveTime(Contest contest, ContestRegistration registration,
            LocalDateTime submissionTime) {
        LocalDateTime startTime;
        if (contest.getTimerMode() == TimerMode.GLOBAL) {
            startTime = contest.getStartTime();
        } else {
            startTime = registration != null && registration.getParticipantStartTime() != null
                    ? registration.getParticipantStartTime()
                    : contest.getStartTime();
        }
        return java.time.Duration.between(startTime, submissionTime).getSeconds();
    }

    private void updateTotalTime(ContestLeaderboardEntry entry, Contest contest,
            ContestRegistration registration, LocalDateTime submissionTime) {
        long totalTime = calculateSolveTime(contest, registration, submissionTime);
        entry.setTotalTimeSeconds(totalTime);
    }

    private ContestLeaderboardEntryDto toDto(ContestLeaderboardEntry entry, Long currentUserId) {
        List<ProblemScoreDto> problemScoreDtos = entry.getProblemScores().stream()
                .map(ps -> new ProblemScoreDto(
                        ps.getProblemId(),
                        ps.getBestPoints(),
                        formatTime(ps.getSolveTimeSeconds())))
                .toList();

        return new ContestLeaderboardEntryDto(
                entry.getRank(),
                entry.getUser().getId(),
                entry.getUser().getName(),
                entry.getTotalPoints(),
                entry.getProblemsSolved(),
                formatTime(entry.getTotalTimeSeconds()),
                problemScoreDtos,
                entry.getMedal(),
                entry.getUser().getId().equals(currentUserId));
    }

    private String formatTime(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private Contest findContestOrThrow(Long contestId) {
        return contestRepository.findById(contestId)
                .orElseThrow(() -> new ContestNotFoundException(contestId));
    }
}
