package com.codearena.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.AddContestProblemRequest;
import com.codearena.dto.ContestDetailDto;
import com.codearena.dto.ContestListDto;
import com.codearena.dto.ContestProblemDetailDto;
import com.codearena.dto.ContestProblemDto;
import com.codearena.dto.CreateContestRequest;
import com.codearena.dto.ProblemOrderRequest;
import com.codearena.dto.UpdateContestRequest;
import com.codearena.dto.ValidationResult;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.User;

public interface ContestService {


    Contest createContest(CreateContestRequest request, User creator);

    Contest updateContest(Long id, UpdateContestRequest request, User editor);

    void deleteContest(Long id, User requester);


    Contest publishContest(Long id, User publisher);

    Contest cancelContest(Long id, String reason, User canceller);


    ContestDetailDto getContestById(Long id, User currentUser);

    ContestDetailDto getContestBySlug(String slug, User currentUser);

    Page<ContestListDto> listPublicContests(ContestStatus status, Pageable pageable);

    Page<ContestListDto> listMyContests(User user, Pageable pageable);

    Page<ContestListDto> listManagedContests(User manager, Pageable pageable);


    ContestProblem addProblem(Long contestId, AddContestProblemRequest request, User manager);

    void removeProblem(Long contestId, Long problemId, User manager);

    void updateProblemOrder(Long contestId, List<ProblemOrderRequest> orders, User manager);

    List<ContestProblemDto> getContestProblems(Long contestId, User currentUser);

    ContestProblemDetailDto getContestProblem(Long contestId, Long problemId, User currentUser);


    ValidationResult validateForPublishing(Long contestId);

    boolean canManageContest(Long contestId, User user);
}
