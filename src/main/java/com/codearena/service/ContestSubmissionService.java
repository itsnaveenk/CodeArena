package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.ContestDraftDto;
import com.codearena.dto.ContestRunResponse;
import com.codearena.dto.ContestSubmissionDto;
import com.codearena.dto.RunRequest;
import com.codearena.dto.SubmitRequest;
import com.codearena.entity.User;

public interface ContestSubmissionService {


    ContestRunResponse runCode(Long contestId, Long problemId, RunRequest request, User user);


    ContestSubmissionDto submit(Long contestId, Long problemId, SubmitRequest request, User user);


    Page<ContestSubmissionDto> getUserSubmissions(Long contestId, User user, Pageable pageable);

    Page<ContestSubmissionDto> getUserSubmissionsForProblem(Long contestId, Long problemId, 
                                                             User user, Pageable pageable);

    ContestSubmissionDto getSubmission(String submissionId, User user);


    void saveDraft(Long contestId, Long problemId, String code, Integer languageId, User user);

    ContestDraftDto getDraft(Long contestId, Long problemId, User user);


    void autoSubmitForUser(Long contestId, Long userId);

    void autoSubmitForContest(Long contestId);
}
