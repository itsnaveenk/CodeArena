package com.codearena.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.codearena.dto.CreateProblemRequest;
import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.ProblemFilter;
import com.codearena.dto.ProblemListDto;
import com.codearena.dto.UpdateProblemRequest;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.User;
import com.codearena.service.ProblemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public ResponseEntity<Page<ProblemListDto>> listProblems(
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String search,
            Pageable pageable,
            @AuthenticationPrincipal User user) {
        
        ProblemFilter filter = new ProblemFilter(difficulty, tags, search);
        Long userId = user != null ? user.getId() : null;
        Page<ProblemListDto> problems = problemService.listPublishedProblems(filter, pageable, userId);
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetailDto> getProblemById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ProblemDetailDto problem = problemService.getProblemById(id, user);
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProblemDetailDto> getProblemBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        ProblemDetailDto problem = problemService.getProblemBySlug(slug, user);
        return ResponseEntity.ok(problem);
    }

    @PostMapping
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<Problem> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal User user) {
        Problem problem = problemService.createProblem(request, user);
        return ResponseEntity.ok(problem);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<Problem> updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProblemRequest request,
            @AuthenticationPrincipal User user) {
        Problem problem = problemService.updateProblem(id, request, user);
        return ResponseEntity.ok(problem);
    }

    @PostMapping("/{id}/request-review")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<Void> requestReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        problemService.requestReview(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        problemService.deleteProblem(id, user);
        return ResponseEntity.noContent().build();
    }
}
