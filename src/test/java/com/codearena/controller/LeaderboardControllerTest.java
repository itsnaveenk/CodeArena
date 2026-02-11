package com.codearena.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codearena.dto.LeaderboardEntryDto;
import com.codearena.dto.LeaderboardPeriod;
import com.codearena.service.LeaderboardService;

@WebMvcTest(LeaderboardController.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @Test
    @DisplayName("GET /api/leaderboards returns list")
    void getLeaderboards_returnsList() throws Exception {
        when(leaderboardService.getLeaderboard(LeaderboardPeriod.ALL, 50))
            .thenReturn(List.of(
                new LeaderboardEntryDto(1, 1L, "Alice", "alice@test.com", 10, 0.05),
                new LeaderboardEntryDto(2, 2L, "Bob", "bob@test.com", 7, 0.10)
            ));

        mockMvc.perform(get("/api/leaderboards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[0].userId").value(1))
            .andExpect(jsonPath("$[0].solved").value(10))
            .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    @DisplayName("GET /api/leaderboards supports period and limit")
    void getLeaderboards_supportsQueryParams() throws Exception {
        when(leaderboardService.getLeaderboard(LeaderboardPeriod.WEEK, 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/leaderboards")
                .param("period", "WEEK")
                .param("limit", "20"))
            .andExpect(status().isOk());
    }
}
