package com.codearena.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.CreateTestcaseRequest;
import com.codearena.dto.TestcaseDto;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.TestcaseRepository;

@Service
@Transactional
public class TestcaseServiceImpl implements TestcaseService {

    private static final Logger log = LoggerFactory.getLogger(TestcaseServiceImpl.class);

    private final TestcaseRepository testcaseRepository;
    private final ProblemRepository problemRepository;

    public TestcaseServiceImpl(TestcaseRepository testcaseRepository, ProblemRepository problemRepository) {
        this.testcaseRepository = testcaseRepository;
        this.problemRepository = problemRepository;
    }

    @Override
    public Testcase addTestcase(Long problemId, CreateTestcaseRequest request, User creator) {
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found with ID: " + problemId));

        if (!canManageTestcases(problem, creator)) {
            throw new AccessDeniedException("You are not authorized to add testcases to this problem");
        }

        Testcase testcase = new Testcase(
            problem,
            request.input(),
            request.expectedOutput(),
            request.isHidden()
        );

        Testcase saved = testcaseRepository.save(testcase);
        log.info("Added testcase ID: {} to problem ID: {} (hidden: {})", 
            saved.getId(), problemId, request.isHidden());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestcaseDto> getTestcases(Long problemId, User requester) {
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found with ID: " + problemId));

        List<Testcase> testcases = testcaseRepository.findByProblemId(problemId);

        boolean canSeeHidden = canSeeHiddenTestcases(problem, requester);

        if (canSeeHidden) {
            return testcases.stream()
                .map(TestcaseDto::fromEntity)
                .toList();
        } else {
            return testcases.stream()
                .filter(tc -> !tc.isHidden())
                .map(TestcaseDto::fromEntity)
                .toList();
        }
    }

    @Override
    public void deleteTestcase(Long testcaseId, User requester) {
        Testcase testcase = testcaseRepository.findById(testcaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Testcase not found with ID: " + testcaseId));

        Problem problem = testcase.getProblem();

        if (!canManageTestcases(problem, requester)) {
            throw new AccessDeniedException("You are not authorized to delete this testcase");
        }

        testcaseRepository.delete(testcase);
        log.info("Deleted testcase ID: {} from problem ID: {}", testcaseId, problem.getId());
    }

    @Override
    public Testcase updateTestcase(Long testcaseId, CreateTestcaseRequest request, User requester) {
        Testcase testcase = testcaseRepository.findById(testcaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Testcase not found with ID: " + testcaseId));

        Problem problem = testcase.getProblem();

        if (!canManageTestcases(problem, requester)) {
            throw new AccessDeniedException("You are not authorized to update this testcase");
        }

        testcase.setInput(request.input());
        testcase.setExpectedOutput(request.expectedOutput());
        testcase.setHidden(request.isHidden());

        Testcase saved = testcaseRepository.save(testcase);
        log.info("Updated testcase ID: {} for problem ID: {} (hidden: {})",
            saved.getId(), problem.getId(), saved.isHidden());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Testcase> getAllTestcasesForExecution(Long problemId) {
        return testcaseRepository.findByProblemId(problemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Testcase> getVisibleTestcases(Long problemId) {
        return testcaseRepository.findByProblemIdAndIsHidden(problemId, false);
    }

    private boolean canManageTestcases(Problem problem, User user) {
        if (user == null) {
            return false;
        }
        
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        return problem.getCreatedBy() != null 
            && problem.getCreatedBy().getId().equals(user.getId());
    }

    private boolean canSeeHiddenTestcases(Problem problem, User user) {
        if (user == null) {
            return false;
        }

        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        return problem.getCreatedBy() != null 
            && problem.getCreatedBy().getId().equals(user.getId());
    }
}
