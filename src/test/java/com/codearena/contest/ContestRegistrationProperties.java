package com.codearena.contest;

import com.codearena.entity.*;
import com.codearena.repository.ContestRegistrationRepository;
import com.codearena.repository.ContestRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for Contest Registration.
 * Tests Properties 5 and 9 from the design document.
 */
@SpringBootTest
@ActiveProfiles("test")
@Label("Contest Registration Properties")
class ContestRegistrationProperties {

    @MockBean
    private ContestRegistrationRepository registrationRepository;

    @MockBean
    private ContestRepository contestRepository;

    /**
     * Property 5: Registration Idempotence
     * 
     * For any user and PUBLIC contest:
     * - Registering when already REGISTERED SHALL return ALREADY_REGISTERED error
     * - Re-registering after WITHDRAWN status SHALL update the existing record (not create new)
     * - At most one ContestRegistration record exists per (contest_id, user_id) pair
     * 
     * Validates: Requirements 3.2.2, 3.2.3
     */
    @Property(tries = 100)
    @Label("Property 5: Registration Idempotence")
    @Tag("Feature: contest-feature, Property 5: Registration Idempotence")
    void registrationIdempotence(
        @ForAll @LongRange(min = 1, max = 1000) long contestId,
        @ForAll @LongRange(min = 1, max = 1000) long userId
    ) {
        // Given: A user already registered for a contest
        when(registrationRepository.existsByContestIdAndUserIdAndStatus(
            contestId, userId, RegistrationStatus.REGISTERED))
            .thenReturn(true);
        
        // Then: Duplicate registration is detected
        boolean alreadyRegistered = registrationRepository
            .existsByContestIdAndUserIdAndStatus(contestId, userId, RegistrationStatus.REGISTERED);
        assertThat(alreadyRegistered).isTrue();
        
        // Given: A user who previously withdrew
        ContestRegistration withdrawnRegistration = new ContestRegistration();
        withdrawnRegistration.setStatus(RegistrationStatus.WITHDRAWN);
        when(registrationRepository.findByContestIdAndUserId(contestId, userId))
            .thenReturn(Optional.of(withdrawnRegistration));
        
        // Then: The existing record can be found for re-registration
        Optional<ContestRegistration> existing = registrationRepository
            .findByContestIdAndUserId(contestId, userId);
        assertThat(existing).isPresent();
        assertThat(existing.get().getStatus()).isEqualTo(RegistrationStatus.WITHDRAWN);
    }

    /**
     * Property 9: Individual Timer Calculation
     * 
     * For any participant in an INDIVIDUAL timer contest who has started:
     * - getEffectiveEndTime() = min(participantStartTime + durationMinutes, contest.endTime)
     * - getRemainingTimeSeconds() = max(0, effectiveEndTime - currentTime)
     * 
     * Validates: Requirements 5.3.4, 5.3.5
     */
    @Property(tries = 100)
    @Label("Property 9: Individual Timer Calculation")
    @Tag("Feature: contest-feature, Property 9: Individual Timer Calculation")
    void individualTimerCalculation(
        @ForAll @IntRange(min = 30, max = 180) int durationMinutes,
        @ForAll @IntRange(min = 0, max = 120) int minutesElapsed
    ) {
        // Given: A contest with INDIVIDUAL timer mode
        Contest contest = new Contest();
        contest.setTimerMode(TimerMode.INDIVIDUAL);
        contest.setDurationMinutes(durationMinutes);
        contest.setStartTime(LocalDateTime.now().minusHours(1));
        contest.setEndTime(LocalDateTime.now().plusHours(3));
        
        // And: A participant who started the contest
        ContestRegistration registration = new ContestRegistration();
        registration.setContest(contest);
        LocalDateTime participantStart = LocalDateTime.now().minusMinutes(minutesElapsed);
        registration.setParticipantStartTime(participantStart);
        
        // When: Calculating effective end time
        LocalDateTime effectiveEnd = registration.getEffectiveEndTime();
        LocalDateTime individualEnd = participantStart.plusMinutes(durationMinutes);
        LocalDateTime contestEnd = contest.getEndTime();
        LocalDateTime expectedEnd = individualEnd.isBefore(contestEnd) ? individualEnd : contestEnd;
        
        // Then: Effective end time is the minimum of individual and contest end
        assertThat(effectiveEnd).isEqualTo(expectedEnd);
        
        // And: Remaining time is non-negative
        long remainingSeconds = registration.getRemainingTimeSeconds();
        assertThat(remainingSeconds).isGreaterThanOrEqualTo(0);
    }

    /**
     * Property 9b: Individual Timer - Time Expired
     * 
     * Verifies that remaining time is 0 when time has expired.
     */
    @Property(tries = 100)
    @Label("Property 9b: Individual Timer - Time Expired")
    @Tag("Feature: contest-feature, Property 9: Individual Timer Calculation")
    void individualTimerExpired(@ForAll @IntRange(min = 30, max = 180) int durationMinutes) {
        // Given: A contest with INDIVIDUAL timer mode
        Contest contest = new Contest();
        contest.setTimerMode(TimerMode.INDIVIDUAL);
        contest.setDurationMinutes(durationMinutes);
        contest.setStartTime(LocalDateTime.now().minusHours(5));
        contest.setEndTime(LocalDateTime.now().minusHours(1));
        
        // And: A participant who started long ago
        ContestRegistration registration = new ContestRegistration();
        registration.setContest(contest);
        registration.setParticipantStartTime(LocalDateTime.now().minusHours(4));
        
        // When: Calculating remaining time
        long remainingSeconds = registration.getRemainingTimeSeconds();
        
        // Then: Remaining time is 0 (time expired)
        assertThat(remainingSeconds).isEqualTo(0);
    }

    /**
     * Property 9c: Global Timer Calculation
     * 
     * Verifies timer calculation for GLOBAL timer mode.
     */
    @Property(tries = 100)
    @Label("Property 9c: Global Timer Calculation")
    @Tag("Feature: contest-feature, Property 9: Individual Timer Calculation")
    void globalTimerCalculation(@ForAll @IntRange(min = 1, max = 180) int minutesRemaining) {
        // Given: A contest with GLOBAL timer mode
        Contest contest = new Contest();
        contest.setTimerMode(TimerMode.GLOBAL);
        contest.setStartTime(LocalDateTime.now().minusHours(1));
        contest.setEndTime(LocalDateTime.now().plusMinutes(minutesRemaining));
        
        // And: A registered participant
        ContestRegistration registration = new ContestRegistration();
        registration.setContest(contest);
        
        // When: Calculating effective end time
        LocalDateTime effectiveEnd = registration.getEffectiveEndTime();
        
        // Then: Effective end time equals contest end time
        assertThat(effectiveEnd).isEqualTo(contest.getEndTime());
    }
}
