package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.document.ContestSubmission;
import com.codearena.dto.ContestLeaderboardEntryDto;
import com.codearena.dto.ContestStatisticsDto;
import com.codearena.entity.User;

public interface ContestLeaderboardService {


    Page<ContestLeaderboardEntryDto> getLeaderboard(Long contestId, Pageable pageable, User currentUser);

    ContestLeaderboardEntryDto getUserRanking(Long contestId, User user);


    void updateUserScore(Long contestId, Long userId, ContestSubmission submission);

    void recalculateAllRankings(Long contestId);

    void assignMedals(Long contestId);


    boolean isLeaderboardFrozen(Long contestId);


    ContestStatisticsDto getStatistics(Long contestId);
}
