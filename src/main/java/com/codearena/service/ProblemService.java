package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.CreateProblemRequest;
import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.ProblemFilter;
import com.codearena.dto.ProblemListDto;
import com.codearena.dto.UpdateProblemRequest;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.User;

public interface ProblemService {

    Page<ProblemListDto> listPublishedProblems(ProblemFilter filter, Pageable pageable, Long userId);

    ProblemDetailDto getProblemById(Long id, User currentUser);

    ProblemDetailDto getProblemBySlug(String slug, User currentUser);

    Problem createProblem(CreateProblemRequest request, User creator);

    Problem updateProblem(Long id, UpdateProblemRequest request, User editor);

    Problem requestReview(Long problemId, User requester);

    Problem publishProblem(Long problemId, User admin);

    Problem rejectProblem(Long problemId, String reason, User admin);

    Problem archiveProblem(Long problemId, User admin);

    Page<ProblemListDto> getPendingProblems(User admin, Pageable pageable);

    com.codearena.dto.ProblemReviewDto getProblemForReview(Long problemId, User admin);

    Page<ProblemDetailDto> listMyProblems(User creator, ProblemStatus status, String search, Pageable pageable);

    void deleteProblem(Long problemId, User requester);
}
