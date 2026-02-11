package com.codearena.contest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestLeaderboardEntry;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ProblemScore;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for Contest entity invariants.
 * Tests Properties 4 and 14 from the design document.
 */
@Label("Contest Entity Properties")
class ContestEntityProperties {

    /**
     * Property 4: Total Points Invariant
     * 
     * For any contest, getTotalPossiblePoints() SHALL equal the sum of all
     * ContestProblem.pointValue for that contest. This invariant must hold
     * after any add/remove problem operation.
     * 
     * Validates: Requirements 2.4.1
     */
    @Property(tries = 100)
    @Label("Property 4: Total Points Invariant")
    @Tag("Feature: contest-feature, Property 4: Total Points Invariant")
    void totalPointsEqualsSum(@ForAll("pointValueLists") List<Integer> pointValues) {
        // Given: A contest with multiple problems
        Contest contest = new Contest();
        contest.setTitle("Test Contest");
        contest.setContestProblems(new ArrayList<>());
        
        int expectedTotal = 0;
        
        // When: Adding problems with various point values
        for (int i = 0; i < pointValues.size(); i++) {
            ContestProblem cp = new ContestProblem();
            cp.setPointValue(pointValues.get(i));
            cp.setDisplayOrder(i + 1);
            cp.setContest(contest);
            contest.getContestProblems().add(cp);
            expectedTotal += pointValues.get(i);
        }
        
        // Then: Total possible points equals sum of all point values
        assertThat(contest.getTotalPossiblePoints()).isEqualTo(expectedTotal);
    }

    /**
     * Property 4b: Total Points Invariant After Removal
     * 
     * Verifies that removing a problem correctly updates the total points.
     */
    @Property(tries = 100)
    @Label("Property 4b: Total Points Invariant After Removal")
    @Tag("Feature: contest-feature, Property 4: Total Points Invariant")
    void totalPointsCorrectAfterRemoval(@ForAll("pointValueLists") List<Integer> pointValues) {
        Assume.that(pointValues.size() >= 2);
        
        // Given: A contest with multiple problems
        Contest contest = new Contest();
        contest.setTitle("Test Contest");
        contest.setContestProblems(new ArrayList<>());
        
        for (int i = 0; i < pointValues.size(); i++) {
            ContestProblem cp = new ContestProblem();
            cp.setPointValue(pointValues.get(i));
            cp.setDisplayOrder(i + 1);
            cp.setContest(contest);
            contest.getContestProblems().add(cp);
        }
        
        int initialTotal = contest.getTotalPossiblePoints();
        
        // When: Removing the first problem
        ContestProblem removed = contest.getContestProblems().remove(0);
        int expectedTotal = initialTotal - removed.getPointValue();
        
        // Then: Total points is reduced by the removed problem's points
        assertThat(contest.getTotalPossiblePoints()).isEqualTo(expectedTotal);
    }

    /**
     * Property 14: Leaderboard Entry Invariants
     * 
     * For any ContestLeaderboardEntry after recalculateTotals():
     * - totalPoints = Σ(problemScores[i].bestPoints)
     * - problemsSolved = count(problemScores where bestPoints > 0)
     * 
     * Validates: Requirements 7.2.2, 7.2.3
     */
    @Property(tries = 100)
    @Label("Property 14: Leaderboard Entry Invariants")
    @Tag("Feature: contest-feature, Property 14: Leaderboard Entry Invariants")
    void leaderboardEntryInvariants(@ForAll("problemScoreLists") List<ProblemScore> problemScores) {
        // Given: A leaderboard entry with problem scores
        ContestLeaderboardEntry entry = new ContestLeaderboardEntry();
        entry.setProblemScores(new ArrayList<>(problemScores));
        
        // Calculate expected values
        double expectedTotalPoints = problemScores.stream()
            .mapToDouble(ProblemScore::getBestPoints)
            .sum();
        
        int expectedProblemsSolved = (int) problemScores.stream()
            .filter(ps -> ps.getBestPoints() > 0)
            .count();
        
        // When: Recalculating totals
        entry.recalculateTotals();
        
        // Then: Totals match expected values
        assertThat(entry.getTotalPoints()).isEqualTo(expectedTotalPoints);
        assertThat(entry.getProblemsSolved()).isEqualTo(expectedProblemsSolved);
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<List<Integer>> pointValueLists() {
        return Arbitraries.integers()
            .between(1, 1000)
            .list()
            .ofMinSize(1)
            .ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<ProblemScore>> problemScoreLists() {
        return problemScore().list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<ProblemScore> problemScore() {
        return Combinators.combine(
            Arbitraries.longs().between(1L, 100L),
            Arbitraries.doubles().between(0.0, 1000.0),
            Arbitraries.longs().between(0L, 86400L)
        ).as(ProblemScore::new);
    }
}
