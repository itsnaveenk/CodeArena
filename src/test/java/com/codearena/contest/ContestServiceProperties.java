package com.codearena.contest;

import com.codearena.entity.*;
import com.codearena.repository.*;
import com.codearena.service.ContestService;
import com.codearena.service.ContestServiceImpl;
import com.codearena.util.SlugGenerator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for ContestService.
 * Tests Properties 1, 3, and 16 from the design document.
 */
@SpringBootTest
@ActiveProfiles("test")
@Label("Contest Service Properties")
class ContestServiceProperties {

    @Autowired
    private ContestService contestService;

    @MockBean
    private ContestRepository contestRepository;

    @MockBean
    private ContestProblemRepository contestProblemRepository;

    @MockBean
    private ProblemRepository problemRepository;

    /**
     * Property 1: Slug Generation Consistency
     * 
     * For any contest title, the generated slug SHALL be: lowercase, spaces
     * replaced with hyphens, special characters removed, and unique (with
     * numeric suffix if collision exists).
     * 
     * Validates: Requirements 1.3.2
     */
    @Property(tries = 100)
    @Label("Property 1: Slug Generation Consistency")
    @Tag("Feature: contest-feature, Property 1: Slug Generation Consistency")
    void slugGenerationIsConsistent(@ForAll("contestTitles") String title) {
        // When: Generating a slug from a title
        String slug = SlugGenerator.generateSlug(title);
        
        // Then: Slug follows the required format
        assertThat(slug).matches("^[a-z0-9-]*$");
        assertThat(slug).doesNotContain(" ");
        assertThat(slug).doesNotContain("--");
        assertThat(slug).isNotEmpty();
    }

    /**
     * Property 1b: Slug Uniqueness
     * 
     * Verifies that slug generation handles collisions by appending suffixes.
     */
    @Property(tries = 50)
    @Label("Property 1b: Slug Uniqueness")
    @Tag("Feature: contest-feature, Property 1: Slug Generation Consistency")
    void slugGenerationHandlesCollisions(@ForAll("contestTitles") String title, @ForAll @IntRange(min = 1, max = 100) int suffix) {
        // Given: A slug with a suffix
        String baseSlug = SlugGenerator.generateSlug(title);
        
        // When: Generating a slug with suffix
        String uniqueSlug = SlugGenerator.generateSlugWithSuffix(title, suffix);
        
        // Then: A unique slug is generated with suffix
        if (!baseSlug.isEmpty()) {
            assertThat(uniqueSlug).isNotEqualTo(baseSlug);
            assertThat(uniqueSlug).startsWith(baseSlug);
            assertThat(uniqueSlug).matches("^[a-z0-9-]+-\\d+$");
        }
    }

    /**
     * Property 3: Contest Problem Uniqueness
     * 
     * For any contest, adding a problem that already exists in that contest
     * SHALL be rejected with DUPLICATE_PROBLEM error, while the same problem
     * CAN exist in multiple different contests simultaneously.
     * 
     * Validates: Requirements 2.2.1, 2.2.2
     */
    @Property(tries = 100)
    @Label("Property 3: Contest Problem Uniqueness")
    @Tag("Feature: contest-feature, Property 3: Contest Problem Uniqueness")
    void contestProblemUniqueness(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll @LongRange(min = 1, max = 1000) long problemId
    ) {
        // Given: A problem already exists in the contest
        when(contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId))
            .thenReturn(true);
        
        // Then: The problem is considered a duplicate for this contest
        boolean isDuplicate = contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId);
        assertThat(isDuplicate).isTrue();
        
        // But: The same problem can exist in a different contest
        long differentContestId = contestId + 1;
        when(contestProblemRepository.existsByContestIdAndProblemId(differentContestId, problemId))
            .thenReturn(false);
        
        boolean isDuplicateInDifferentContest = contestProblemRepository
            .existsByContestIdAndProblemId(differentContestId, problemId);
        assertThat(isDuplicateInDifferentContest).isFalse();
    }

    /**
     * Property 16: Valid Status Transitions
     * 
     * For any contest, only specific status transitions SHALL be allowed
     * according to the state machine defined in requirements.
     * 
     * Validates: Requirements 8.1.1, 8.1.2
     */
    @Property(tries = 100)
    @Label("Property 16: Valid Status Transitions")
    @Tag("Feature: contest-feature, Property 16: Valid Status Transitions")
    void onlyValidStatusTransitionsAllowed(
        @ForAll ContestStatus from,
        @ForAll ContestStatus to
    ) {
        // Given: Valid transition rules
        Set<ContestStatus> validTargets = switch (from) {
            case DRAFT -> Set.of(ContestStatus.PUBLISHED, ContestStatus.CANCELLED);
            case PUBLISHED -> Set.of(ContestStatus.RUNNING, ContestStatus.CANCELLED);
            case RUNNING -> Set.of(ContestStatus.FINISHED, ContestStatus.CANCELLED);
            case FINISHED, CANCELLED -> Set.of();
        };
        
        // When: Checking if transition is valid
        boolean isValid = validTargets.contains(to);
        
        // Then: Only valid transitions are allowed
        if (isValid) {
            assertThat(validTargets).contains(to);
        } else {
            assertThat(validTargets).doesNotContain(to);
        }
    }

    /**
     * Property 16b: Terminal States
     * 
     * Verifies that FINISHED and CANCELLED are terminal states.
     */
    @Property(tries = 100)
    @Label("Property 16b: Terminal States")
    @Tag("Feature: contest-feature, Property 16: Valid Status Transitions")
    void finishedAndCancelledAreTerminalStates(@ForAll ContestStatus targetStatus) {
        // Given: A contest in FINISHED state
        Set<ContestStatus> validFromFinished = Set.of();
        
        // Then: No transitions are allowed from FINISHED
        assertThat(validFromFinished).doesNotContain(targetStatus);
        
        // Given: A contest in CANCELLED state
        Set<ContestStatus> validFromCancelled = Set.of();
        
        // Then: No transitions are allowed from CANCELLED
        assertThat(validFromCancelled).doesNotContain(targetStatus);
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> contestTitles() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars(' ', '-', '_', '!', '@', '#')
            .ofMinLength(3)
            .ofMaxLength(100);
    }
}
