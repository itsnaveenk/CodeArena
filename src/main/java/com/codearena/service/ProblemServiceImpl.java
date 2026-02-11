package com.codearena.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.codearena.dto.CreateProblemRequest;
import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.ProblemFilter;
import com.codearena.dto.ProblemListDto;
import com.codearena.dto.ProblemSolveStatus;
import com.codearena.dto.TestcaseDto;
import com.codearena.dto.UpdateProblemRequest;
import com.codearena.entity.Problem;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.StarterCode;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.entity.Verdict;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.StarterCodeRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.TestcaseRepository;
import com.codearena.util.SlugGenerator;

@Service
@Transactional(readOnly = true)
public class ProblemServiceImpl implements ProblemService {

    private static final Logger log = LoggerFactory.getLogger(ProblemServiceImpl.class);

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final TestcaseRepository testcaseRepository;
    private final StarterCodeRepository starterCodeRepository;
    private final AdminAuditService adminAuditService;

    public ProblemServiceImpl(ProblemRepository problemRepository, 
                              SubmissionRepository submissionRepository,
                              TestcaseRepository testcaseRepository,
                              StarterCodeRepository starterCodeRepository,
                              AdminAuditService adminAuditService) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.testcaseRepository = testcaseRepository;
        this.starterCodeRepository = starterCodeRepository;
        this.adminAuditService = adminAuditService;
    }

    @Override
    public Page<ProblemListDto> listPublishedProblems(ProblemFilter filter, Pageable pageable, Long userId) {
        log.debug("Listing published problems with filter: {}, userId: {}", filter, userId);
        Specification<Problem> spec = buildSpecification(filter);
        Page<Problem> problems = problemRepository.findAll(spec, pageable);
        if (userId != null) {
            return problems.map(problem -> {
                ProblemSolveStatus status = calculateSolveStatus(userId, problem.getId());
                return ProblemListDto.fromEntity(problem, status);
            });
        }
        return problems.map(ProblemListDto::fromEntity);
    }

    private Specification<Problem> buildSpecification(ProblemFilter filter) {
        List<Specification<Problem>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.equal(root.get("status"), ProblemStatus.PUBLISHED));
        if (filter != null) {
            if (filter.hasDifficulty()) {
                specs.add((root, query, cb) -> cb.equal(root.get("difficulty"), filter.difficulty()));
            }
            if (filter.hasSearch()) {
                String searchPattern = "%" + filter.search().toLowerCase() + "%";
                specs.add((root, query, cb) -> cb.like(cb.lower(root.get("title")), searchPattern));
            }
            if (filter.hasTags()) {
                specs.add(buildTagsSpecification(filter.tags()));
            }
        }
        return specs.stream().reduce(Specification::and).orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Problem> buildTagsSpecification(List<String> tags) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> tagPredicates = new ArrayList<>();
            for (String tag : tags) {
                tagPredicates.add(cb.isTrue(cb.function("JSON_CONTAINS", Boolean.class, root.get("tags"), cb.literal("\"" + tag + "\""), cb.literal("$"))));
            }
            return cb.or(tagPredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ProblemSolveStatus calculateSolveStatus(Long userId, Long problemId) {
        if (submissionRepository.existsByUserIdAndProblemIdAndVerdict(userId, problemId, Verdict.ACCEPTED)) {
            return ProblemSolveStatus.SOLVED;
        }
        if (submissionRepository.existsByUserIdAndProblemId(userId, problemId)) {
            return ProblemSolveStatus.ATTEMPTED;
        }
        return ProblemSolveStatus.NOT_TRIED;
    }

    @Override
    public ProblemDetailDto getProblemById(Long id, User currentUser) {
        log.debug("Getting problem by ID: {}, user: {}", id, currentUser != null ? currentUser.getEmail() : "anonymous");
        Problem problem = problemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Problem", id));
        return buildProblemDetailDto(problem, currentUser);
    }

    @Override
    public ProblemDetailDto getProblemBySlug(String slug, User currentUser) {
        log.debug("Getting problem by slug: {}, user: {}", slug, currentUser != null ? currentUser.getEmail() : "anonymous");
        Problem problem = problemRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Problem", slug));
        return buildProblemDetailDto(problem, currentUser);
    }

    private ProblemDetailDto buildProblemDetailDto(Problem problem, User currentUser) {
        if (!canAccessProblem(problem, currentUser)) {
            throw new ResourceNotFoundException("Problem not found");
        }
        boolean canSeeHiddenTestcases = isAdmin(currentUser);
        List<TestcaseDto> examples = getTestcasesForUser(problem.getId(), canSeeHiddenTestcases);
        Map<String, String> starterCode = getStarterCodeMap(problem.getId());
        return ProblemDetailDto.fromEntity(problem, examples, starterCode);
    }

    private boolean canAccessProblem(Problem problem, User currentUser) {
        if (problem.getStatus() == ProblemStatus.PUBLISHED) return true;
        if (currentUser == null) return false;
        if (isAdmin(currentUser)) return true;
        return isProblemSetter(currentUser) && isCreator(problem, currentUser);
    }

    private boolean isAdmin(User user) { return user != null && user.getRole() == Role.ADMIN; }
    private boolean isProblemSetter(User user) { return user != null && user.getRole() == Role.PROBLEM_SETTER; }
    private boolean isCreator(Problem problem, User user) { return user != null && problem.getCreatedBy() != null && problem.getCreatedBy().getId().equals(user.getId()); }

    private List<TestcaseDto> getTestcasesForUser(Long problemId, boolean includeHidden) {
        List<Testcase> testcases = testcaseRepository.findByProblemId(problemId);
        List<TestcaseDto> result = new ArrayList<>();
        for (Testcase testcase : testcases) {
            if (testcase.isHidden() && !includeHidden) continue;
            result.add(TestcaseDto.fromEntity(testcase));
        }
        return result;
    }

    private Map<String, String> getStarterCodeMap(Long problemId) {
        List<StarterCode> starterCodes = starterCodeRepository.findByProblemId(problemId);
        Map<String, String> result = new HashMap<>();
        for (StarterCode sc : starterCodes) { result.put(String.valueOf(sc.getLanguageId()), sc.getCode()); }
        return result;
    }

    @Override
    @Transactional
    public Problem createProblem(CreateProblemRequest request, User creator) {
        log.debug("Creating problem with title: {}, creator: {}", request.title(), creator.getEmail());
        String slug = SlugGenerator.generateSlug(request.title());
        Problem problem = new Problem(request.title(), slug, request.statement(), request.difficulty(), creator);
        if (request.constraints() != null) problem.setConstraints(request.constraints());
        if (request.hasTags()) problem.setTags(new ArrayList<>(request.tags()));
        problem = problemRepository.save(problem);
        if (request.hasStarterCode()) {
            for (Map.Entry<String, String> entry : request.starterCode().entrySet()) {
                int languageId = Integer.parseInt(entry.getKey());
                StarterCode starterCode = new StarterCode(problem, languageId, entry.getValue());
                problem.addStarterCode(starterCode);
            }
            problem = problemRepository.save(problem);
        }
        log.info("Created problem with ID: {}, slug: {}", problem.getId(), problem.getSlug());
        return problem;
    }

    @Override
    @Transactional
    public Problem updateProblem(Long id, UpdateProblemRequest request, User editor) {
        log.debug("Updating problem ID: {}, editor: {}", id, editor.getEmail());
        Problem problem = problemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Problem", id));
        checkEditAuthorization(problem, editor);
        checkEditableStatus(problem, editor);
        if (request.hasTitle()) { problem.setTitle(request.title()); problem.setSlug(generateUniqueSlugExcluding(request.title(), id)); }
        if (request.hasStatement()) problem.setStatement(request.statement());
        if (request.hasConstraints()) problem.setConstraints(request.constraints());
        if (request.hasDifficulty()) problem.setDifficulty(request.difficulty());
        if (request.hasTags()) problem.setTags(new ArrayList<>(request.tags()));
        if (request.hasStarterCode()) updateStarterCode(problem, request.starterCode());
        problem = problemRepository.save(problem);
        log.info("Updated problem ID: {}", problem.getId());
        return problem;
    }

    private void checkEditAuthorization(Problem problem, User editor) {
        if (isAdmin(editor)) return;
        if (isCreator(problem, editor)) return;
        throw new AccessDeniedException("You can only edit your own problems");
    }

    private void checkEditableStatus(Problem problem, User editor) {
        if (isAdmin(editor)) return;
        ProblemStatus status = problem.getStatus();
        if (status != ProblemStatus.DRAFT && status != ProblemStatus.REJECTED) {
            throw new IllegalStateException("Can only edit problems in DRAFT or REJECTED status. Current status: " + status);
        }
    }

    private String generateUniqueSlugExcluding(String title, Long excludeId) {
        String baseSlug = SlugGenerator.generateSlug(title);
        String slug = baseSlug;
        int suffix = 0;
        while (true) {
            var existing = problemRepository.findBySlug(slug);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) break;
            suffix++;
            slug = SlugGenerator.generateSlugWithSuffix(title, suffix);
        }
        return slug;
    }

    private void updateStarterCode(Problem problem, Map<String, String> starterCodeMap) {
        problem.getStarterCodes().clear();
        for (Map.Entry<String, String> entry : starterCodeMap.entrySet()) {
            int languageId = Integer.parseInt(entry.getKey());
            StarterCode starterCode = new StarterCode(problem, languageId, entry.getValue());
            problem.addStarterCode(starterCode);
        }
    }

    @Override
    @Transactional
    public Problem requestReview(Long problemId, User requester) {
        log.debug("Requesting review for problem ID: {}, requester: {}", problemId, requester.getEmail());
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        if (!isAdmin(requester) && !isCreator(problem, requester)) throw new AccessDeniedException("Only the problem creator or an admin can request review");
        if (problem.getStatus() != ProblemStatus.DRAFT) throw new IllegalStateException("Can only request review for problems in DRAFT status. Current status: " + problem.getStatus());
        long visibleCount = testcaseRepository.countByProblemIdAndIsHidden(problemId, false);
        long hiddenCount = testcaseRepository.countByProblemIdAndIsHidden(problemId, true);
        if (visibleCount < 1) throw new IllegalStateException("Problem must have at least one visible testcase before requesting review");
        if (hiddenCount < 1) throw new IllegalStateException("Problem must have at least one hidden testcase before requesting review");
        problem.setStatus(ProblemStatus.PENDING_REVIEW);
        problem = problemRepository.save(problem);
        log.info("Problem ID: {} submitted for review", problemId);
        return problem;
    }

    @Override
    @Transactional
    public Problem publishProblem(Long problemId, User admin) {
        log.debug("Publishing problem ID: {}, admin: {}", problemId, admin.getEmail());
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can publish problems");
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        if (problem.getStatus() != ProblemStatus.PENDING_REVIEW) throw new IllegalStateException("Can only publish problems in PENDING_REVIEW status. Current status: " + problem.getStatus());
        problem.setStatus(ProblemStatus.PUBLISHED);
        problem = problemRepository.save(problem);
        try { adminAuditService.record(admin, com.codearena.entity.AdminAuditAction.PROBLEM_PUBLISHED, com.codearena.entity.AdminAuditTargetType.PROBLEM, String.valueOf(problemId), java.util.Map.of("title", problem.getTitle(), "slug", problem.getSlug())); } catch (Exception ignored) {}
        log.info("Problem ID: {} published by admin: {}", problemId, admin.getEmail());
        return problem;
    }

    @Override
    @Transactional
    public Problem rejectProblem(Long problemId, String reason, User admin) {
        log.debug("Rejecting problem ID: {}, admin: {}, reason: {}", problemId, admin.getEmail(), reason);
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can reject problems");
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        if (problem.getStatus() != ProblemStatus.PENDING_REVIEW) throw new IllegalStateException("Can only reject problems in PENDING_REVIEW status. Current status: " + problem.getStatus());
        problem.setStatus(ProblemStatus.REJECTED);
        problem.setRejectionReason(reason);
        problem.setRejectedAt(LocalDateTime.now());
        problem.setRejectedBy(admin);
        problem = problemRepository.save(problem);
        try { adminAuditService.record(admin, com.codearena.entity.AdminAuditAction.PROBLEM_REJECTED, com.codearena.entity.AdminAuditTargetType.PROBLEM, String.valueOf(problemId), java.util.Map.of("title", problem.getTitle(), "slug", problem.getSlug(), "reason", reason)); } catch (Exception ignored) {}
        log.info("Problem ID: {} rejected by admin: {} with reason: {}", problemId, admin.getEmail(), reason);
        return problem;
    }

    @Override
    @Transactional
    public Problem archiveProblem(Long problemId, User admin) {
        log.debug("Archiving problem ID: {}, admin: {}", problemId, admin.getEmail());
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can archive problems");
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        problem.setStatus(ProblemStatus.ARCHIVED);
        problem = problemRepository.save(problem);
        try { adminAuditService.record(admin, com.codearena.entity.AdminAuditAction.PROBLEM_ARCHIVED, com.codearena.entity.AdminAuditTargetType.PROBLEM, String.valueOf(problemId), java.util.Map.of("title", problem.getTitle(), "slug", problem.getSlug())); } catch (Exception ignored) {}
        log.info("Problem ID: {} archived by admin: {}", problemId, admin.getEmail());
        return problem;
    }

    @Override
    public Page<ProblemListDto> getPendingProblems(User admin, Pageable pageable) {
        log.debug("Getting pending problems, admin: {}", admin.getEmail());
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can view pending problems");
        Page<Problem> problems = problemRepository.findByStatus(ProblemStatus.PENDING_REVIEW, pageable);
        return problems.map(ProblemListDto::fromEntity);
    }

    @Override
    public com.codearena.dto.ProblemReviewDto getProblemForReview(Long problemId, User admin) {
        log.debug("Getting problem for review, ID: {}, admin: {}", problemId, admin.getEmail());
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can review problems");
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        List<com.codearena.dto.TestcaseDto> testcaseDtos = testcaseRepository.findByProblemId(problemId).stream().map(com.codearena.dto.TestcaseDto::fromEntity).toList();
        Map<String, String> starterCodeMap = new java.util.HashMap<>();
        List<StarterCode> starterCodes = starterCodeRepository.findByProblemId(problemId);
        for (StarterCode sc : starterCodes) starterCodeMap.put(String.valueOf(sc.getLanguageId()), sc.getCode());
        long submissionCount = submissionRepository.countByProblemId(problemId);
        return com.codearena.dto.ProblemReviewDto.fromEntity(problem, testcaseDtos, starterCodeMap, submissionCount);
    }

    @Override
    public Page<ProblemDetailDto> listMyProblems(User creator, ProblemStatus status, String search, Pageable pageable) {
        if (creator == null) throw new AccessDeniedException("Authentication required");
        if (!isAdmin(creator) && !isProblemSetter(creator)) throw new AccessDeniedException("Only problem setters can view their problems");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasStatus = status != null;
        boolean hasSearch = StringUtils.hasText(search);
        Page<Problem> problems;
        if (!hasStatus && !hasSearch) {
            problems = problemRepository.findByCreatedById(creator.getId(), sortedPageable);
        } else {
            Specification<Problem> spec = (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), creator.getId());
            if (hasStatus) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
            if (hasSearch) { String pattern = "%" + search.toLowerCase() + "%"; spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern)); }
            problems = problemRepository.findAll(spec, sortedPageable);
        }
        return problems.map(p -> ProblemDetailDto.fromEntity(p, List.of(), Map.of()));
    }

    @Override
    @Transactional
    public void deleteProblem(Long problemId, User requester) {
        if (requester == null) throw new AccessDeniedException("Authentication required");
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        if (!isAdmin(requester)) {
            if (!isCreator(problem, requester)) throw new AccessDeniedException("You can only delete your own problems");
            if (problem.getStatus() != ProblemStatus.DRAFT) throw new IllegalStateException("Can only delete problems in DRAFT status. Current status: " + problem.getStatus());
        }
        problemRepository.delete(problem);
        log.info("Deleted problem ID: {} by {}", problemId, requester.getEmail());
    }
}
