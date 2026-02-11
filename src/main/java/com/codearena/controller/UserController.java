package com.codearena.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.SubmissionDto;
import com.codearena.dto.UserDto;
import com.codearena.dto.UserStatsDto;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.User;
import com.codearena.service.ProblemService;
import com.codearena.service.SubmissionService;
import com.codearena.service.UserService;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;
    private final SubmissionService submissionService;
    private final ProblemService problemService;

    public UserController(UserService userService, SubmissionService submissionService, ProblemService problemService) {
        this.userService = userService;
        this.submissionService = submissionService;
        this.problemService = problemService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        UserDto userDto = userService.getCurrentUser(user);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserStatsDto> getUserStats(@AuthenticationPrincipal User user) {
        UserStatsDto stats = submissionService.getUserStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/submissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<SubmissionDto>> getUserSubmissions(
            @RequestParam(required = false) Long problemId,
            Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<SubmissionDto> submissions = submissionService.getUserSubmissions(
            user.getId(), problemId, pageable, user);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/problems")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ProblemDetailDto>> getMyProblems(
            @RequestParam(required = false) ProblemStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<ProblemDetailDto> problems = problemService.listMyProblems(user, status, search, pageable);
        return ResponseEntity.ok(problems);
    }
}
