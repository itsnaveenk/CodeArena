package com.codearena.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.AdminAuditEventDto;
import com.codearena.dto.BulkActionRequest;
import com.codearena.dto.PlatformStatsDto;
import com.codearena.dto.ProblemListDto;
import com.codearena.dto.ProblemManagementDto;
import com.codearena.dto.SubmissionDto;
import com.codearena.dto.UserListDto;
import com.codearena.entity.AdminAuditAction;
import com.codearena.entity.Difficulty;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.service.AdminAuditService;
import com.codearena.service.AdminService;
import com.codearena.service.ProblemService;
import com.codearena.service.SubmissionService;
import com.codearena.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProblemService problemService;
    private final UserService userService;
    private final SubmissionService submissionService;
    private final AdminService adminService;
    private final AdminAuditService adminAuditService;

    public AdminController(ProblemService problemService, 
                           UserService userService,
                           SubmissionService submissionService,
                           AdminService adminService,
                           AdminAuditService adminAuditService) {
        this.problemService = problemService;
        this.userService = userService;
        this.submissionService = submissionService;
        this.adminService = adminService;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDto> getPlatformStats() {
        PlatformStatsDto stats = adminService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserListDto>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<UserListDto> users = adminService.listUsers(role, search, pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal User admin) {
        userService.updateRole(id, request.role(), admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/problems")
    public ResponseEntity<Page<ProblemManagementDto>> getAllProblems(
            @RequestParam(required = false) ProblemStatus status,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<ProblemManagementDto> problems = adminService.listAllProblems(status, difficulty, search, pageable);
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/problems/pending")
    public ResponseEntity<Page<ProblemListDto>> getPendingProblems(
            Pageable pageable,
            @AuthenticationPrincipal User admin) {
        Page<ProblemListDto> problems = problemService.getPendingProblems(admin, pageable);
        return ResponseEntity.ok(problems);
    }

    @PostMapping("/problems/{id}/publish")
    public ResponseEntity<Void> publishProblem(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        problemService.publishProblem(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/problems/{id}/reject")
    public ResponseEntity<Void> rejectProblem(
            @PathVariable Long id,
            @Valid @RequestBody com.codearena.dto.ProblemRejectRequest request,
            @AuthenticationPrincipal User user) {
        problemService.rejectProblem(id, request.reason(), user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/problems/{id}/archive")
    public ResponseEntity<Void> archiveProblem(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        problemService.archiveProblem(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/problems/{id}/review")
    public ResponseEntity<com.codearena.dto.ProblemReviewDto> getProblemForReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        com.codearena.dto.ProblemReviewDto review = problemService.getProblemForReview(id, admin);
        return ResponseEntity.ok(review);
    }

    @PostMapping("/problems/bulk/publish")
    public ResponseEntity<BulkActionResponse> bulkPublishProblems(
            @Valid @RequestBody BulkActionRequest request,
            @AuthenticationPrincipal User admin) {
        int count = adminService.bulkPublish(request.ids(), admin);
        return ResponseEntity.ok(new BulkActionResponse(count, "published"));
    }

    @PostMapping("/problems/bulk/reject")
    public ResponseEntity<BulkActionResponse> bulkRejectProblems(
            @Valid @RequestBody BulkActionRequest request,
            @AuthenticationPrincipal User admin) {
        int count = adminService.bulkReject(request.ids(), admin);
        return ResponseEntity.ok(new BulkActionResponse(count, "rejected"));
    }

    @PostMapping("/problems/bulk/archive")
    public ResponseEntity<BulkActionResponse> bulkArchiveProblems(
            @Valid @RequestBody BulkActionRequest request,
            @AuthenticationPrincipal User admin) {
        int count = adminService.bulkArchive(request.ids(), admin);
        return ResponseEntity.ok(new BulkActionResponse(count, "archived"));
    }

    @GetMapping("/submissions")
    public ResponseEntity<Page<SubmissionDto>> getAllSubmissions(
            @RequestParam(required = false) Verdict verdict,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long problemId,
            Pageable pageable) {
        Page<SubmissionDto> submissions = submissionService.getAllSubmissionsFiltered(
            verdict, userId, problemId, pageable);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AdminAuditEventDto>> getAuditEvents(
            @RequestParam(required = false) AdminAuditAction action,
            @RequestParam(required = false) Long actorUserId,
            Pageable pageable) {
        Page<AdminAuditEventDto> events = adminAuditService.list(action, actorUserId, pageable);
        return ResponseEntity.ok(events);
    }

    public record RoleUpdateRequest(@NotNull Role role) {}

    public record BulkActionResponse(int count, String action) {}
}
