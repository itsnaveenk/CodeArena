package com.codearena.service;

import java.util.List;

import com.codearena.dto.LeaderboardEntryDto;
import com.codearena.dto.LeaderboardPeriod;

public interface LeaderboardService {

    List<LeaderboardEntryDto> getLeaderboard(LeaderboardPeriod period, int limit);
}
