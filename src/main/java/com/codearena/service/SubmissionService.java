package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.document.Submission;
import com.codearena.dto.SubmissionDto;
import com.codearena.dto.SubmitRequest;
import com.codearena.dto.UserStatsDto;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;

public interface SubmissionService {

    Submission recordSubmission(SubmitRequest request, User user, Verdict verdict,
                                Double runtime, Integer memory, int passed, int total);

    Page<SubmissionDto> getUserSubmissions(Long userId, Long problemId, Pageable pageable, User requester);

    SubmissionDto getSubmission(String id, User requester);

    Page<SubmissionDto> getAllSubmissions(Pageable pageable);

    Page<SubmissionDto> getAllSubmissionsFiltered(Verdict verdict, Long userId, Long problemId, Pageable pageable);

    UserStatsDto getUserStats(Long userId);
}
