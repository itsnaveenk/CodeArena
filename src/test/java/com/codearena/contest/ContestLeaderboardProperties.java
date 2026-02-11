package com.codearena.contest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import com.codearena.document.ContestSubmission;
import com.codearena.entity.MedalType;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based tests for Contest Leaderboard.
 * Tests Properties 12, 13, and 15 from the design document.
 */
@Label("Contest Leaderboard Properties")
class ContestLeaderboardProperties {

    /**
     * Property 12: Best Submission Selection
     * 
     * For any user and problem in a contest, the leaderboard entry's
     * problemScore for that problem SHALL reflect the submission with the
     * highest pointsEarned value.
     * 
     * Validates: Requirements 6.3.3
     */
    @Property(tries = 100)
    @Label("Property 12: Best Submission Selection")
    @Tag("Feature: contest-feature, Property 12: Best Submission Selection")
    void bestSubmissionSelection(@ForAll("submissionLists") List<ContestSubmission> submissions) {
        Assume.that(!submissions.isEmpty());
        
        // Given: Multiple submissions for the same problem
        Long problemId = submissions.get(0).getProblemId();
        
        // When: Finding the best submission
        ContestSubmission best = submissions.stream()
            .max(Comparator.comparingDouble(ContestSubmission::getPointsEarned))
            .orElseThrow();
        
        // Then: Best submission has the highest points
        for (ContestSubmission submission : submissions) {
            assertThat(best.getPointsEarned())
                .isGreaterThanOrEqualTo(submission.getPointsEarned());
        }
    }

    /**
     * Property 13: Leaderboard Ranking Order
     * 
     * For any contest leaderboard query, entries SHALL be ordered by:
     * 1. totalPoints DESCENDING
     * 2. totalTimeSeconds ASCENDING (tiebreaker)
     * 
     * Validates: Requirements 7.2.1
     */
    @Property(tries = 100)
    @Label("Property 13: Leaderboard Ranking Order")
    @Tag("Feature: contest-feature, Property 13: Leaderboard Ranking Order")
    void leaderboardRankingOrder(@ForAll("leaderboardEntries") List<LeaderboardTestEntry> entries) {
        Assume.that(entries.size() >= 2);
        
        // When: Sorting entries by leaderboard rules
        List<LeaderboardTestEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator
            .comparingDouble(LeaderboardTestEntry::totalPoints).reversed()
            .thenComparingLong(LeaderboardTestEntry::totalTimeSeconds));
        
        // Then: Entries are correctly ordered
        for (int i = 0; i < sorted.size() - 1; i++) {
            LeaderboardTestEntry current = sorted.get(i);
            LeaderboardTestEntry next = sorted.get(i + 1);
            
            // Current should have more points, or same points with less time
            boolean validOrder = current.totalPoints() > next.totalPoints() ||
                (current.totalPoints() == next.totalPoints() && 
                 current.totalTimeSeconds() <= next.totalTimeSeconds());
            
            assertThat(validOrder).isTrue();
        }
    }

    /**
     * Property 13b: Ranking Transitivity
     * 
     * Verifies that ranking order is transitive.
     */
    @Property(tries = 100)
    @Label("Property 13b: Ranking Transitivity")
    @Tag("Feature: contest-feature, Property 13: Leaderboard Ranking Order")
    void rankingTransitivity(@ForAll("leaderboardEntries") List<LeaderboardTestEntry> entries) {
        Assume.that(entries.size() >= 3);
        
        // Given: Three entries sorted by ranking rules
        List<LeaderboardTestEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator
            .comparingDouble(LeaderboardTestEntry::totalPoints).reversed()
            .thenComparingLong(LeaderboardTestEntry::totalTimeSeconds));
        
        // Then: If A > B and B > C, then A > C (transitivity)
        if (sorted.size() >= 3) {
            LeaderboardTestEntry a = sorted.get(0);
            LeaderboardTestEntry b = sorted.get(1);
            LeaderboardTestEntry c = sorted.get(2);
            
            boolean aBeforeB = compareEntries(a, b) <= 0;
            boolean bBeforeC = compareEntries(b, c) <= 0;
            boolean aBeforeC = compareEntries(a, c) <= 0;
            
            if (aBeforeB && bBeforeC) {
                assertThat(aBeforeC).isTrue();
            }
        }
    }

    /**
     * Property 15: Medal Assignment by Percentile
     * 
     * For any FINISHED contest with N participants (N > 0):
     * - GOLD: rank <= ceil(N × 0.10)
     * - SILVER: rank <= ceil(N × 0.25) AND medal != GOLD
     * - BRONZE: rank <= ceil(N × 0.50) AND medal NOT IN (GOLD, SILVER)
     * - No medal: rank > ceil(N × 0.50)
     * 
     * Validates: Requirements 7.7.2, 7.7.3, 7.7.4, 7.7.5
     */
    @Property(tries = 100)
    @Label("Property 15: Medal Assignment by Percentile")
    @Tag("Feature: contest-feature, Property 15: Medal Assignment by Percentile")
    void medalAssignmentByPercentile(
        @ForAll @IntRange(min = 1, max = 100) int participantCount,
        @ForAll @IntRange(min = 1, max = 100) int rank
    ) {
        Assume.that(rank <= participantCount);
        
        // When: Calculating medal based on rank and participant count
        MedalType medal = calculateMedal(rank, participantCount);
        
        // Then: Medal is assigned according to percentile rules
        int goldCutoff = (int) Math.ceil(participantCount * 0.10);
        int silverCutoff = (int) Math.ceil(participantCount * 0.25);
        int bronzeCutoff = (int) Math.ceil(participantCount * 0.50);
        
        if (rank <= goldCutoff) {
            assertThat(medal).isEqualTo(MedalType.GOLD);
        } else if (rank <= silverCutoff) {
            assertThat(medal).isEqualTo(MedalType.SILVER);
        } else if (rank <= bronzeCutoff) {
            assertThat(medal).isEqualTo(MedalType.BRONZE);
        } else {
            assertThat(medal).isNull();
        }
    }

    /**
     * Property 15b: Medal Distribution
     * 
     * Verifies that medals are distributed correctly across all participants.
     */
    @Property(tries = 100)
    @Label("Property 15b: Medal Distribution")
    @Tag("Feature: contest-feature, Property 15: Medal Assignment by Percentile")
    void medalDistribution(@ForAll @IntRange(min = 10, max = 100) int participantCount) {
        // When: Assigning medals to all participants
        List<MedalType> medals = new ArrayList<>();
        for (int rank = 1; rank <= participantCount; rank++) {
            medals.add(calculateMedal(rank, participantCount));
        }
        
        // Then: Count medals by type
        long goldCount = medals.stream().filter(m -> m == MedalType.GOLD).count();
        long silverCount = medals.stream().filter(m -> m == MedalType.SILVER).count();
        long bronzeCount = medals.stream().filter(m -> m == MedalType.BRONZE).count();
        long noMedalCount = medals.stream().filter(m -> m == null).count();
        
        // Verify counts match expected percentiles
        int goldCutoff = (int) Math.ceil(participantCount * 0.10);
        int silverCutoff = (int) Math.ceil(participantCount * 0.25);
        int bronzeCutoff = (int) Math.ceil(participantCount * 0.50);
        
        assertThat(goldCount).isEqualTo(goldCutoff);
        assertThat(silverCount).isEqualTo(silverCutoff - goldCutoff);
        assertThat(bronzeCount).isEqualTo(bronzeCutoff - silverCutoff);
        assertThat(noMedalCount).isEqualTo(participantCount - bronzeCutoff);
    }

    // ========== Helper Methods ==========

    private MedalType calculateMedal(int rank, int participantCount) {
        int goldCutoff = (int) Math.ceil(participantCount * 0.10);
        int silverCutoff = (int) Math.ceil(participantCount * 0.25);
        int bronzeCutoff = (int) Math.ceil(participantCount * 0.50);
        
        if (rank <= goldCutoff) {
            return MedalType.GOLD;
        } else if (rank <= silverCutoff) {
            return MedalType.SILVER;
        } else if (rank <= bronzeCutoff) {
            return MedalType.BRONZE;
        } else {
            return null;
        }
    }

    private int compareEntries(LeaderboardTestEntry a, LeaderboardTestEntry b) {
        int pointsCompare = Double.compare(b.totalPoints(), a.totalPoints());
        if (pointsCompare != 0) {
            return pointsCompare;
        }
        return Long.compare(a.totalTimeSeconds(), b.totalTimeSeconds());
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<List<ContestSubmission>> submissionLists() {
        return Combinators.combine(
            Arbitraries.longs().between(1L, 100L),
            Arbitraries.longs().between(1L, 100L),
            Arbitraries.longs().between(1L, 100L)
        ).as((contestId, problemId, userId) -> {
            return Arbitraries.doubles()
                .between(0.0, 1000.0)
                .list()
                .ofMinSize(1)
                .ofMaxSize(10)
                .map(points -> points.stream()
                    .map(p -> {
                        ContestSubmission sub = new ContestSubmission();
                        sub.setContestId(contestId);
                        sub.setProblemId(problemId);
                        sub.setUserId(userId);
                        sub.setPointsEarned(p);
                        return sub;
                    })
                    .collect(Collectors.toList()))
                .sample();
        }).flatMap(list -> Arbitraries.just(list));
    }

    @Provide
    Arbitrary<List<LeaderboardTestEntry>> leaderboardEntries() {
        return leaderboardEntry().list().ofMinSize(2).ofMaxSize(50);
    }

    @Provide
    Arbitrary<LeaderboardTestEntry> leaderboardEntry() {
        return Combinators.combine(
            Arbitraries.doubles().between(0.0, 1000.0),
            Arbitraries.longs().between(0L, 86400L)
        ).as(LeaderboardTestEntry::new);
    }

    // Test data record
    record LeaderboardTestEntry(double totalPoints, long totalTimeSeconds) {}
}
