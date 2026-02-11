package com.codearena.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.ContestListDto;
import com.codearena.entity.User;
import com.codearena.service.ContestService;

@RestController
@RequestMapping("/api/me/contests")
public class UserContestController {

    private final ContestService contestService;

    public UserContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    public ResponseEntity<Page<ContestListDto>> getMyContests(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<ContestListDto> contests = contestService.listMyContests(user, pageable);
        return ResponseEntity.ok(contests);
    }
}
