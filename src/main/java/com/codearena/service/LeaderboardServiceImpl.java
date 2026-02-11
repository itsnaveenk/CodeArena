package com.codearena.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.codearena.dto.LeaderboardEntryDto;
import com.codearena.dto.LeaderboardPeriod;
import com.codearena.entity.User;
import com.codearena.repository.LeaderboardRepository;
import com.codearena.repository.UserRepository;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;

    public LeaderboardServiceImpl(LeaderboardRepository leaderboardRepository, UserRepository userRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "leaderboards", key = "#period.name() + '_' + #limit")
    public List<LeaderboardEntryDto> getLeaderboard(LeaderboardPeriod period, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        LocalDateTime from = switch (period == null ? LeaderboardPeriod.ALL : period) {
            case ALL -> null;
            case WEEK -> LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            case MONTH -> LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        };

        List<LeaderboardRepository.LeaderboardRow> rows = leaderboardRepository.topSolved(from, safeLimit);

        List<Long> userIds = rows.stream().map(LeaderboardRepository.LeaderboardRow::userId).toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<LeaderboardEntryDto> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRepository.LeaderboardRow row = rows.get(i);
            User u = usersById.get(row.userId());

            out.add(new LeaderboardEntryDto(
                    i + 1,
                    row.userId(),
                    u != null ? u.getName() : "Unknown",
                    u != null ? u.getEmail() : null,
                    row.solved(),
                    row.bestRuntime()));
        }

        return out;
    }
}
