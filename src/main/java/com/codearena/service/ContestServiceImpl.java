package com.codearena.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.AddContestProblemRequest;
import com.codearena.dto.ContestDetailDto;
import com.codearena.dto.ContestListDto;
import com.codearena.dto.ContestProblemDetailDto;
import com.codearena.dto.ContestProblemDto;
import com.codearena.dto.CreateContestRequest;
import com.codearena.dto.ProblemOrderRequest;
import com.codearena.dto.TestcaseDto;
import com.codearena.dto.UpdateContestRequest;
import com.codearena.dto.ValidationResult;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.ContestVisibility;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.RegistrationStatus;
import com.codearena.entity.Role;
import com.codearena.entity.StarterCode;
import com.codearena.entity.Testcase;
import com.codearena.entity.TimerMode;
import com.codearena.entity.User;
import com.codearena.exception.ContestNotFoundException;
import com.codearena.exception.ContestValidationException;
import com.codearena.exception.InvalidStatusTransitionException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRegistrationRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.StarterCodeRepository;
import com.codearena.repository.TestcaseRepository;
import com.codearena.util.SlugGenerator;

@Service
@Transactional(readOnly = true)
public class ContestServiceImpl implements ContestService {

    private static final Logger log = LoggerFactory.getLogger(ContestServiceImpl.class);

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestRegistrationRepository contestRegistrationRepository;
    private final ProblemRepository problemRepository;
    private final TestcaseRepository testcaseRepository;
    private final StarterCodeRepository starterCodeRepository;

    public ContestServiceImpl(ContestRepository contestRepository,
                              ContestProblemRepository contestProblemRepository,
                              ContestRegistrationRepository contestRegistrationRepository,
                              ProblemRepository problemRepository,
                              TestcaseRepository testcaseRepository,
                              StarterCodeRepository starterCodeRepository) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestRegistrationRepository = contestRegistrationRepository;
        this.problemRepository = problemRepository;
        this.testcaseRepository = testcaseRepository;
        this.starterCodeRepository = starterCodeRepository;
    }


    @Override
    @Transactional
    public Contest createContest(CreateContestRequest request, User creator) {
        log.debug("Creating contest with title: {}, creator: {}", request.title(), creator.getEmail());

        String slug = generateUniqueSlug(request.title());

        Contest contest = new Contest(
            request.title(),
            slug,
            request.description(),
            request.timerMode(),
            request.startTime(),
            request.endTime(),
            creator
        );

        if (request.hasVisibility()) {
            contest.setVisibility(request.visibility());
        }
        if (request.hasCategory()) {
            contest.setCategory(request.category());
        }
        if (request.hasScoringModel()) {
            contest.setScoringModel(request.scoringModel());
        }
        if (request.hasRegistrationStartTime()) {
            contest.setRegistrationStartTime(request.registrationStartTime());
        }
        if (request.hasRegistrationEndTime()) {
            contest.setRegistrationEndTime(request.registrationEndTime());
        }
        if (request.hasMaxParticipants()) {
            contest.setMaxParticipants(request.maxParticipants());
        }
        if (request.hasLeaderboardFreezeMinutes()) {
            contest.setLeaderboardFreezeMinutes(request.leaderboardFreezeMinutes());
        }
        if (request.hasLeaderboardDelaySeconds()) {
            contest.setLeaderboardDelaySeconds(request.leaderboardDelaySeconds());
        }

        if (request.timerMode() == TimerMode.INDIVIDUAL) {
            if (request.durationMinutes() == null) {
                throw new ContestValidationException("Duration is required for INDIVIDUAL timer mode");
            }
            contest.setDurationMinutes(request.durationMinutes());
        }

        contest = contestRepository.save(contest);
        log.info("Created contest with ID: {}, slug: {}", contest.getId(), contest.getSlug());
        return contest;
    }

    @Override
    @Transactional
    public Contest updateContest(Long id, UpdateContestRequest request, User editor) {
        log.debug("Updating contest ID: {}, editor: {}", id, editor.getEmail());

        Contest contest = contestRepository.findById(id)
            .orElseThrow(() -> new ContestNotFoundException(id));

        checkManageAuthorization(contest, editor);

        checkEditRestrictions(contest, editor, request);

        if (request.hasTitle() && contest.getStatus() == ContestStatus.DRAFT) {
            contest.setTitle(request.title());
            contest.setSlug(generateUniqueSlugExcluding(request.title(), id));
        }
        if (request.hasDescription()) {
            contest.setDescription(request.description());
        }
        if (request.hasRegistrationStartTime()) {
            validateRegistrationTimeUpdate(contest);
            contest.setRegistrationStartTime(request.registrationStartTime());
        }
        if (request.hasRegistrationEndTime()) {
            validateRegistrationTimeUpdate(contest);
            contest.setRegistrationEndTime(request.registrationEndTime());
        }

        contest = contestRepository.save(contest);
        log.info("Updated contest ID: {}", contest.getId());
        return contest;
    }

    @Override
    @Transactional
    public void deleteContest(Long id, User requester) {
        log.debug("Deleting contest ID: {}, requester: {}", id, requester.getEmail());

        Contest contest = contestRepository.findById(id)
            .orElseThrow(() -> new ContestNotFoundException(id));

        checkManageAuthorization(contest, requester);

        if (contest.getStatus() != ContestStatus.DRAFT) {
            throw ContestValidationException.cannotDelete(contest.getStatus().name());
        }

        contestRepository.delete(contest);
        log.info("Deleted contest ID: {} by {}", id, requester.getEmail());
    }


    @Override
    @Transactional
    public Contest publishContest(Long id, User publisher) {
        log.debug("Publishing contest ID: {}, publisher: {}", id, publisher.getEmail());

        Contest contest = contestRepository.findById(id)
            .orElseThrow(() -> new ContestNotFoundException(id));

        checkManageAuthorization(contest, publisher);

        if (contest.getStatus() != ContestStatus.DRAFT) {
            throw new InvalidStatusTransitionException(contest.getStatus(), ContestStatus.PUBLISHED);
        }

        ValidationResult validation = validateForPublishing(id);
        if (!validation.valid()) {
            throw new ContestValidationException(validation.errors());
        }

        contest.setStatus(ContestStatus.PUBLISHED);
        contest = contestRepository.save(contest);

        log.info("Published contest ID: {} by {}", id, publisher.getEmail());
        return contest;
    }

    @Override
    @Transactional
    public Contest cancelContest(Long id, String reason, User canceller) {
        log.debug("Cancelling contest ID: {}, canceller: {}, reason: {}", id, canceller.getEmail(), reason);

        Contest contest = contestRepository.findById(id)
            .orElseThrow(() -> new ContestNotFoundException(id));

        checkManageAuthorization(contest, canceller);

        ContestStatus currentStatus = contest.getStatus();
        if (currentStatus == ContestStatus.FINISHED || currentStatus == ContestStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(currentStatus, ContestStatus.CANCELLED);
        }

        contest.setStatus(ContestStatus.CANCELLED);
        contest = contestRepository.save(contest);


        log.info("Cancelled contest ID: {} by {} with reason: {}", id, canceller.getEmail(), reason);
        return contest;
    }


    @Override
    public ContestDetailDto getContestById(Long id, User currentUser) {
        log.debug("Getting contest by ID: {}, user: {}", id, currentUser != null ? currentUser.getEmail() : "anonymous");

        Contest contest = contestRepository.findById(id)
            .orElseThrow(() -> new ContestNotFoundException(id));

        return buildContestDetailDto(contest, currentUser);
    }

    @Override
    public ContestDetailDto getContestBySlug(String slug, User currentUser) {
        log.debug("Getting contest by slug: {}, user: {}", slug, currentUser != null ? currentUser.getEmail() : "anonymous");

        Contest contest = contestRepository.findBySlug(slug)
            .orElseThrow(() -> new ContestNotFoundException(slug));

        return buildContestDetailDto(contest, currentUser);
    }

    @Override
    public Page<ContestListDto> listPublicContests(ContestStatus status, Pageable pageable) {
        log.debug("Listing public contests, status filter: {}", status);

        Page<Contest> contests;
        if (status != null) {
            contests = contestRepository.findByStatus(status, pageable);
            contests = contests.map(c -> c.getVisibility() == ContestVisibility.PUBLIC ? c : null);
        } else {
            contests = contestRepository.findPublicContests(pageable);
        }

        return contests.map(ContestListDto::fromEntity);
    }

    @Override
    public Page<ContestListDto> listMyContests(User user, Pageable pageable) {
        log.debug("Listing contests for user: {}", user.getEmail());

        Page<ContestRegistration> registrations = contestRegistrationRepository.findByUserId(user.getId(), pageable);

        return registrations.map(reg -> {
            Contest contest = reg.getContest();
            boolean isRegistered = reg.getStatus() == RegistrationStatus.REGISTERED;
            return ContestListDto.fromEntity(contest, isRegistered, reg.getStatus());
        });
    }

    @Override
    public Page<ContestListDto> listManagedContests(User manager, Pageable pageable) {
        log.debug("Listing managed contests for user: {}", manager.getEmail());

        Page<Contest> contests = contestRepository.findByCreatedById(manager.getId(), pageable);
        return contests.map(ContestListDto::fromEntity);
    }


    @Override
    @Transactional
    public ContestProblem addProblem(Long contestId, AddContestProblemRequest request, User manager) {
        log.debug("Adding problem {} to contest {}, manager: {}", request.problemId(), contestId, manager.getEmail());

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        checkManageAuthorization(contest, manager);

        if (!contest.canModifyProblems()) {
            throw ContestValidationException.cannotModifyProblems(contest.getStatus().name());
        }

        Problem problem = problemRepository.findById(request.problemId())
            .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        if (problem.getStatus() != ProblemStatus.PUBLISHED && problem.getStatus() != ProblemStatus.PENDING_REVIEW) {
            throw ContestValidationException.invalidProblemForContest();
        }

        if (contestProblemRepository.existsByContestIdAndProblemId(contestId, request.problemId())) {
            throw ContestValidationException.duplicateProblem();
        }

        Integer displayOrder = request.displayOrder();
        if (displayOrder == null) {
            displayOrder = contestProblemRepository.findMaxDisplayOrder(contestId).orElse(0) + 1;
        }

        ContestProblem contestProblem = new ContestProblem(contest, problem, request.pointValue(), displayOrder);
        contestProblem = contestProblemRepository.save(contestProblem);

        log.info("Added problem {} to contest {} with display order {}", request.problemId(), contestId, displayOrder);
        return contestProblem;
    }

    @Override
    @Transactional
    public void removeProblem(Long contestId, Long problemId, User manager) {
        log.debug("Removing problem {} from contest {}, manager: {}", problemId, contestId, manager.getEmail());

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        checkManageAuthorization(contest, manager);

        if (!contest.canModifyProblems()) {
            throw ContestValidationException.cannotModifyProblems(contest.getStatus().name());
        }

        if (!contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId)) {
            throw new ResourceNotFoundException("ContestProblem", "contestId=" + contestId + ", problemId=" + problemId);
        }

        contestProblemRepository.deleteByContestIdAndProblemId(contestId, problemId);

        log.info("Removed problem {} from contest {}", problemId, contestId);
    }

    @Override
    @Transactional
    public void updateProblemOrder(Long contestId, List<ProblemOrderRequest> orders, User manager) {
        log.debug("Updating problem order for contest {}, manager: {}", contestId, manager.getEmail());

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        checkManageAuthorization(contest, manager);

        if (!contest.canModifyProblems()) {
            throw ContestValidationException.cannotModifyProblems(contest.getStatus().name());
        }

        for (ProblemOrderRequest order : orders) {
            ContestProblem contestProblem = contestProblemRepository
                .findByContestIdAndProblemId(contestId, order.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("ContestProblem", 
                    "contestId=" + contestId + ", problemId=" + order.problemId()));
            
            contestProblem.setDisplayOrder(order.displayOrder());
            contestProblemRepository.save(contestProblem);
        }

        log.info("Updated problem order for contest {}", contestId);
    }

    @Override
    public List<ContestProblemDto> getContestProblems(Long contestId, User currentUser) {
        log.debug("Getting problems for contest {}, user: {}", contestId, 
            currentUser != null ? currentUser.getEmail() : "anonymous");

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        if (!canViewProblems(contest, currentUser)) {
            return List.of(); // Return empty list if problems are not visible
        }

        List<ContestProblem> contestProblems = contestProblemRepository.findByContestIdOrderByDisplayOrderAsc(contestId);

        return contestProblems.stream()
            .map(ContestProblemDto::fromEntity)
            .toList();
    }

    @Override
    public ContestProblemDetailDto getContestProblem(Long contestId, Long problemId, User currentUser) {
        log.debug("Getting problem {} for contest {}, user: {}", problemId, contestId,
            currentUser != null ? currentUser.getEmail() : "anonymous");

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        if (!canViewProblems(contest, currentUser)) {
            throw new AccessDeniedException("Problems are not visible yet");
        }

        ContestProblem contestProblem = contestProblemRepository.findByContestIdAndProblemId(contestId, problemId)
            .orElseThrow(() -> new ResourceNotFoundException("ContestProblem", 
                "contestId=" + contestId + ", problemId=" + problemId));

        List<TestcaseDto> examples = getExamples(problemId);

        Map<String, String> starterCode = getStarterCodeMap(problemId);

        return ContestProblemDetailDto.fromEntity(contestProblem, examples, starterCode);
    }


    @Override
    public ValidationResult validateForPublishing(Long contestId) {
        log.debug("Validating contest {} for publishing", contestId);

        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ContestNotFoundException(contestId));

        ValidationResult.Builder builder = ValidationResult.builder();

        int problemCount = contestProblemRepository.countByContestId(contestId);
        builder.addErrorIf(problemCount == 0, "At least one problem is required");

        LocalDateTime now = LocalDateTime.now();
        builder.addErrorIf(!contest.getStartTime().isAfter(now), "Start time must be in the future");

        builder.addErrorIf(!contest.getEndTime().isAfter(contest.getStartTime()), 
            "End time must be after start time");

        if (contest.getRegistrationStartTime() != null && contest.getRegistrationEndTime() != null) {
            builder.addErrorIf(contest.getRegistrationStartTime().isAfter(contest.getRegistrationEndTime()),
                "Registration start time must be before or equal to registration end time");
            builder.addErrorIf(contest.getRegistrationEndTime().isAfter(contest.getStartTime()),
                "Registration must end before or at contest start time");
        }

        if (contest.getTimerMode() == TimerMode.INDIVIDUAL) {
            if (contest.getDurationMinutes() == null) {
                builder.addError("Duration is required for INDIVIDUAL timer mode");
            } else {
                long contestDurationMinutes = java.time.Duration.between(
                    contest.getStartTime(), contest.getEndTime()).toMinutes();
                builder.addErrorIf(contest.getDurationMinutes() > contestDurationMinutes,
                    "Duration cannot exceed contest window");
            }
        }

        List<ContestProblem> contestProblems = contestProblemRepository.findByContestIdOrderByDisplayOrderAsc(contestId);
        for (ContestProblem cp : contestProblems) {
            Problem problem = cp.getProblem();
            ProblemStatus status = problem.getStatus();
            if (status != ProblemStatus.PUBLISHED && status != ProblemStatus.PENDING_REVIEW) {
                builder.addError("Problem '" + problem.getTitle() + "' must be PUBLISHED or PENDING_REVIEW");
            }
        }

        return builder.build();
    }

    @Override
    public boolean canManageContest(Long contestId, User user) {
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        Contest contest = contestRepository.findById(contestId).orElse(null);
        if (contest == null) {
            return false;
        }
        return isCreator(contest, user);
    }


    private String generateUniqueSlug(String title) {
        String baseSlug = SlugGenerator.generateSlug(title);
        String slug = baseSlug;
        int suffix = 0;
        while (contestRepository.existsBySlug(slug)) {
            suffix++;
            slug = SlugGenerator.generateSlugWithSuffix(title, suffix);
        }
        return slug;
    }

    private String generateUniqueSlugExcluding(String title, Long excludeId) {
        String baseSlug = SlugGenerator.generateSlug(title);
        String slug = baseSlug;
        int suffix = 0;
        while (true) {
            Optional<Contest> existing = contestRepository.findBySlug(slug);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                break;
            }
            suffix++;
            slug = SlugGenerator.generateSlugWithSuffix(title, suffix);
        }
        return slug;
    }

    private void checkManageAuthorization(Contest contest, User user) {
        if (isAdmin(user)) {
            return;
        }
        if (!isProblemSetter(user)) {
            throw new AccessDeniedException("Only PROBLEM_SETTERs and ADMINs can manage contests");
        }
        if (!isCreator(contest, user)) {
            throw new AccessDeniedException("You can only manage contests you created");
        }
    }

    private void checkEditRestrictions(Contest contest, User editor, UpdateContestRequest request) {
        ContestStatus status = contest.getStatus();

        switch (status) {
            case DRAFT:
                break;
            case PUBLISHED:
                if (request.hasTitle()) {
                    throw ContestValidationException.cannotModify("PUBLISHED (title cannot be changed)");
                }
                break;
            case RUNNING:
                if (!isAdmin(editor)) {
                    throw ContestValidationException.cannotModify("RUNNING");
                }
                break;
            case FINISHED:
            case CANCELLED:
                throw ContestValidationException.cannotModify(status.name());
        }
    }

    private void validateRegistrationTimeUpdate(Contest contest) {
        if (contest.getStatus() == ContestStatus.PUBLISHED) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(contest.getRegistrationStartTime())) {
                throw new ContestValidationException("Cannot modify registration times after registration has started");
            }
        }
    }

    private ContestDetailDto buildContestDetailDto(Contest contest, User currentUser) {
        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            if (currentUser == null) {
                throw new ContestNotFoundException(contest.getId());
            }
            if (!canManageContest(contest.getId(), currentUser) && 
                !isRegisteredOrInvited(contest.getId(), currentUser.getId())) {
                throw new ContestNotFoundException(contest.getId());
            }
        }

        ContestRegistration registration = null;
        if (currentUser != null) {
            registration = contestRegistrationRepository
                .findByContestIdAndUserId(contest.getId(), currentUser.getId())
                .orElse(null);
        }

        return ContestDetailDto.fromEntity(contest, registration);
    }

    private boolean isRegisteredOrInvited(Long contestId, Long userId) {
        return contestRegistrationRepository.findByContestIdAndUserId(contestId, userId).isPresent();
    }

    private boolean canViewProblems(Contest contest, User currentUser) {
        ContestStatus status = contest.getStatus();

        if (currentUser != null && canManageContest(contest.getId(), currentUser)) {
            return true;
        }

        if (status == ContestStatus.DRAFT) {
            return false;
        }

        if (status == ContestStatus.PUBLISHED) {
            return false;
        }

        if (status == ContestStatus.RUNNING) {
            if (currentUser == null) {
                return false;
            }
            Optional<ContestRegistration> registration = contestRegistrationRepository
                .findByContestIdAndUserId(contest.getId(), currentUser.getId());
            if (registration.isEmpty() || registration.get().getStatus() != RegistrationStatus.REGISTERED) {
                return false;
            }
            if (contest.getTimerMode() == TimerMode.INDIVIDUAL) {
                return registration.get().hasStarted();
            }
            return true;
        }

        return status == ContestStatus.FINISHED;
    }

    private List<TestcaseDto> getExamples(Long problemId) {
        List<Testcase> testcases = testcaseRepository.findByProblemId(problemId);
        List<TestcaseDto> examples = new ArrayList<>();
        for (Testcase testcase : testcases) {
            if (!testcase.isHidden()) {
                examples.add(TestcaseDto.fromEntity(testcase));
            }
        }
        return examples;
    }

    private Map<String, String> getStarterCodeMap(Long problemId) {
        List<StarterCode> starterCodes = starterCodeRepository.findByProblemId(problemId);
        Map<String, String> result = new HashMap<>();
        for (StarterCode sc : starterCodes) {
            result.put(String.valueOf(sc.getLanguageId()), sc.getCode());
        }
        return result;
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private boolean isProblemSetter(User user) {
        return user != null && user.getRole() == Role.PROBLEM_SETTER;
    }

    private boolean isCreator(Contest contest, User user) {
        return user != null && contest.getCreatedBy() != null && 
               contest.getCreatedBy().getId().equals(user.getId());
    }
}
