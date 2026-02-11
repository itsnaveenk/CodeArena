package com.codearena.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.ContestEditorialDto;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestEditorial;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.User;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.ContestValidationException;
import com.codearena.repository.ContestEditorialRepository;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;

@Service
@Transactional(readOnly = true)
public class ContestEditorialServiceImpl implements ContestEditorialService {

    private static final Logger log = LoggerFactory.getLogger(ContestEditorialServiceImpl.class);
    private static final int MAX_CONTENT_LENGTH = 10000;

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestEditorialRepository contestEditorialRepository;

    public ContestEditorialServiceImpl(ContestRepository contestRepository,
                                        ContestProblemRepository contestProblemRepository,
                                        ContestEditorialRepository contestEditorialRepository) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestEditorialRepository = contestEditorialRepository;
    }

    @Override
    @Transactional
    public ContestEditorial createOrUpdateEditorial(Long contestId, Long problemId, String content, User author) {
        log.debug("Creating/updating editorial for contest {} problem {}", contestId, problemId);

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        if (contest.getStatus() != ContestStatus.FINISHED) {
            throw new ContestValidationException(
                "Editorials can only be added after contest is FINISHED. Current status: " + contest.getStatus());
        }

        ContestProblem contestProblem = contestProblemRepository.findByContestIdAndProblemId(contestId, problemId)
            .orElseThrow(() -> new ContestValidationException(
                "Problem " + problemId + " is not part of contest " + contestId));

        if (content == null || content.isBlank()) {
            throw new ContestValidationException("Editorial content cannot be empty");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ContestValidationException(
                "Editorial content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters");
        }

        ContestEditorial editorial = contestEditorialRepository
            .findByContestIdAndProblemId(contestId, problemId)
            .orElse(null);

        if (editorial != null) {
            editorial.setContent(content);
        } else {
            editorial = new ContestEditorial(contest, contestProblem.getProblem(), content);
        }

        return contestEditorialRepository.save(editorial);
    }

    @Override
    public List<ContestEditorialDto> getEditorials(Long contestId) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return contestEditorialRepository.findByContestId(contestId).stream()
            .map(ContestEditorialDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public ContestEditorialDto getEditorial(Long contestId, Long problemId) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException(contestId);
        }

        return contestEditorialRepository.findByContestIdAndProblemId(contestId, problemId)
            .map(ContestEditorialDto::fromEntity)
            .orElse(null);
    }
}
