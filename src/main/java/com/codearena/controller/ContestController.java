package com.codearena.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.AddContestProblemRequest;
import com.codearena.dto.BulkInviteResult;
import com.codearena.dto.ContestDetailDto;
import com.codearena.dto.ContestEditorialDto;
import com.codearena.dto.ContestInvitationDto;
import com.codearena.dto.ContestLeaderboardEntryDto;
import com.codearena.dto.ContestListDto;
import com.codearena.dto.ContestProblemDetailDto;
import com.codearena.dto.ContestProblemDto;
import com.codearena.dto.ContestRegistrationDto;
import com.codearena.dto.ContestRunResponse;
import com.codearena.dto.ContestStatisticsDto;
import com.codearena.dto.ContestSubmissionDto;
import com.codearena.dto.CreateContestRequest;
import com.codearena.dto.ProblemOrderRequest;
import com.codearena.dto.RunRequest;
import com.codearena.dto.SubmitRequest;
import com.codearena.dto.UpdateContestRequest;
import com.codearena.dto.ValidationResult;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestEditorial;
import com.codearena.entity.ContestInvitation;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.User;
import com.codearena.service.ContestEditorialService;
import com.codearena.service.ContestInvitationService;
import com.codearena.service.ContestLeaderboardService;
import com.codearena.service.ContestRegistrationService;
import com.codearena.service.ContestService;
import com.codearena.service.ContestSubmissionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;
    private final ContestRegistrationService registrationService;
    private final ContestInvitationService invitationService;
    private final ContestSubmissionService submissionService;
    private final ContestLeaderboardService leaderboardService;
    private final ContestEditorialService editorialService;

    public ContestController(
            ContestService contestService,
            ContestRegistrationService registrationService,
            ContestInvitationService invitationService,
            ContestSubmissionService submissionService,
            ContestLeaderboardService leaderboardService,
            ContestEditorialService editorialService) {
        this.contestService = contestService;
        this.registrationService = registrationService;
        this.invitationService = invitationService;
        this.submissionService = submissionService;
        this.leaderboardService = leaderboardService;
        this.editorialService = editorialService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestDetailDto> createContest(
            @Valid @RequestBody CreateContestRequest request,
            @AuthenticationPrincipal User user) {
        Contest contest = contestService.createContest(request, user);
        ContestDetailDto dto = contestService.getContestById(contest.getId(), user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<ContestListDto>> listContests(
            @RequestParam(required = false) ContestStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<ContestListDto> contests = contestService.listPublicContests(status, pageable);
        return ResponseEntity.ok(contests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContestDetailDto> getContest(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ContestDetailDto contest = contestService.getContestById(id, user);
        return ResponseEntity.ok(contest);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ContestDetailDto> getContestBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        ContestDetailDto contest = contestService.getContestBySlug(slug, user);
        return ResponseEntity.ok(contest);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestDetailDto> updateContest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContestRequest request,
            @AuthenticationPrincipal User user) {
        contestService.updateContest(id, request, user);
        ContestDetailDto dto = contestService.getContestById(id, user);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<Void> deleteContest(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        contestService.deleteContest(id, user);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestDetailDto> publishContest(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        contestService.publishContest(id, user);
        ContestDetailDto dto = contestService.getContestById(id, user);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestDetailDto> cancelContest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User user) {
        contestService.cancelContest(id, reason, user);
        ContestDetailDto dto = contestService.getContestById(id, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ValidationResult> validateContest(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ValidationResult result = contestService.validateForPublishing(id);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/{id}/problems")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestProblemDto> addProblem(
            @PathVariable Long id,
            @Valid @RequestBody AddContestProblemRequest request,
            @AuthenticationPrincipal User user) {
        ContestProblem contestProblem = contestService.addProblem(id, request, user);
        ContestProblemDto dto = ContestProblemDto.fromEntity(contestProblem);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}/problems/{problemId}")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<Void> removeProblem(
            @PathVariable Long id,
            @PathVariable Long problemId,
            @AuthenticationPrincipal User user) {
        contestService.removeProblem(id, problemId, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/problems/order")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<Void> updateProblemOrder(
            @PathVariable Long id,
            @Valid @RequestBody List<ProblemOrderRequest> orders,
            @AuthenticationPrincipal User user) {
        contestService.updateProblemOrder(id, orders, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/problems")
    public ResponseEntity<List<ContestProblemDto>> getProblems(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        List<ContestProblemDto> problems = contestService.getContestProblems(id, user);
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/{id}/problems/{problemId}")
    public ResponseEntity<ContestProblemDetailDto> getProblem(
            @PathVariable Long id,
            @PathVariable Long problemId,
            @AuthenticationPrincipal User user) {
        ContestProblemDetailDto problem = contestService.getContestProblem(id, problemId, user);
        return ResponseEntity.ok(problem);
    }


    @PostMapping("/{id}/register")
    public ResponseEntity<ContestRegistrationDto> register(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ContestRegistration registration = registrationService.register(id, user);
        ContestRegistrationDto dto = ContestRegistrationDto.fromEntity(registration);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> withdraw(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        registrationService.withdraw(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ContestRegistrationDto> startContest(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        registrationService.startContest(id, user);
        ContestRegistrationDto dto = registrationService.getRegistration(id, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/registration")
    public ResponseEntity<ContestRegistrationDto> getRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ContestRegistrationDto dto = registrationService.getRegistration(id, user);
        return ResponseEntity.ok(dto);
    }


    @PostMapping("/{id}/invitations")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestInvitationDto> invite(
            @PathVariable Long id,
            @RequestParam String email,
            @AuthenticationPrincipal User user) {
        ContestInvitation invitation = invitationService.invite(id, email, user);
        ContestInvitationDto dto = ContestInvitationDto.fromEntity(invitation);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/invitations/bulk")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<List<BulkInviteResult>> bulkInvite(
            @PathVariable Long id,
            @RequestBody @Size(max = 100) List<String> emails,
            @AuthenticationPrincipal User user) {
        List<BulkInviteResult> results = invitationService.bulkInvite(id, emails, user);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/invitations")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<Page<ContestInvitationDto>> getInvitations(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<ContestInvitationDto> invitations = invitationService.getContestInvitations(id, pageable);
        return ResponseEntity.ok(invitations);
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable Long id,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal User user) {
        invitationService.revokeInvitation(invitationId, user);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/problems/{problemId}/run")
    public ResponseEntity<ContestRunResponse> runCode(
            @PathVariable Long id,
            @PathVariable Long problemId,
            @Valid @RequestBody RunRequest request,
            @AuthenticationPrincipal User user) {
        ContestRunResponse response = submissionService.runCode(id, problemId, request, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/problems/{problemId}/submit")
    public ResponseEntity<ContestSubmissionDto> submitForProblem(
            @PathVariable Long id,
            @PathVariable Long problemId,
            @Valid @RequestBody SubmitRequest request,
            @AuthenticationPrincipal User user) {
        ContestSubmissionDto submission = submissionService.submit(id, problemId, request, user);
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/{id}/submissions")
    public ResponseEntity<ContestSubmissionDto> submit(
            @PathVariable Long id,
            @RequestParam(required = false) Long problemId,
            @Valid @RequestBody SubmitRequest request,
            @AuthenticationPrincipal User user) {
        Long pid = problemId != null ? problemId : request.problemId();
        ContestSubmissionDto submission = submissionService.submit(id, pid, request, user);
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<Page<ContestSubmissionDto>> getSubmissions(
            @PathVariable Long id,
            @RequestParam(required = false) Long problemId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<ContestSubmissionDto> submissions;
        if (problemId != null) {
            submissions = submissionService.getUserSubmissionsForProblem(id, problemId, user, pageable);
        } else {
            submissions = submissionService.getUserSubmissions(id, user, pageable);
        }
        return ResponseEntity.ok(submissions);
    }



    @PutMapping("/{id}/problems/{problemId}/draft")
    public ResponseEntity<Void> saveDraft(
            @PathVariable Long id,
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User user) {
        String code = (String) request.get("code");
        Integer languageId = request.get("languageId") instanceof Number n ? n.intValue() : null;
        submissionService.saveDraft(id, problemId, code, languageId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/problems/{problemId}/draft")
    public ResponseEntity<com.codearena.dto.ContestDraftDto> getDraft(
            @PathVariable Long id,
            @PathVariable Long problemId,
            @AuthenticationPrincipal User user) {
        com.codearena.dto.ContestDraftDto draft = submissionService.getDraft(id, problemId, user);
        if (draft == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(draft);
    }


    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<Page<ContestLeaderboardEntryDto>> getLeaderboard(
            @PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<ContestLeaderboardEntryDto> leaderboard = leaderboardService.getLeaderboard(id, pageable, user);
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/{id}/leaderboard/me")
    public ResponseEntity<ContestLeaderboardEntryDto> getMyRanking(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ContestLeaderboardEntryDto ranking = leaderboardService.getUserRanking(id, user);
        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<ContestStatisticsDto> getStatistics(
            @PathVariable Long id) {
        ContestStatisticsDto statistics = leaderboardService.getStatistics(id);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/{id}/editorials")
    public ResponseEntity<List<ContestEditorialDto>> getEditorials(
            @PathVariable Long id) {
        List<ContestEditorialDto> editorials = editorialService.getEditorials(id);
        return ResponseEntity.ok(editorials);
    }

    @PostMapping("/{id}/editorials")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    public ResponseEntity<ContestEditorialDto> createEditorial(
            @PathVariable Long id,
            @RequestParam Long problemId,
            @RequestBody @Size(max = 10000) String content,
            @AuthenticationPrincipal User user) {
        ContestEditorial editorial = editorialService.createOrUpdateEditorial(id, problemId, content, user);
        ContestEditorialDto dto = ContestEditorialDto.fromEntity(editorial);
        return ResponseEntity.ok(dto);
    }
}
