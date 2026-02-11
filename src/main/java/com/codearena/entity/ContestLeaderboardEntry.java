package com.codearena.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "contest_leaderboard_entries",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_contest_user_leaderboard",
           columnNames = {"contest_id", "user_id"}
       ),
       indexes = {
           @Index(name = "idx_leaderboard_ranking",
                  columnList = "contest_id, total_points DESC, total_time_seconds ASC")
       })
public class ContestLeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rank_position", nullable = false)
    private Integer rank = 0;

    @Column(name = "total_points", nullable = false)
    private Double totalPoints = 0.0;

    @Column(name = "problems_solved", nullable = false)
    private Integer problemsSolved = 0;

    @Column(name = "total_time_seconds", nullable = false)
    private Long totalTimeSeconds = 0L;

    @Column(name = "problem_scores", columnDefinition = "CLOB")
    @Convert(converter = ProblemScoresConverter.class)
    private List<ProblemScore> problemScores = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MedalType medal;

    @Column(name = "last_submission_time")
    private LocalDateTime lastSubmissionTime;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    public ContestLeaderboardEntry() {
    }

    public ContestLeaderboardEntry(Contest contest, User user) {
        this.contest = contest;
        this.user = user;
    }

    public void recalculateTotals() {
        this.totalPoints = problemScores.stream()
            .mapToDouble(ProblemScore::getBestPoints)
            .sum();
        this.problemsSolved = (int) problemScores.stream()
            .filter(ps -> ps.getBestPoints() > 0)
            .count();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contest getContest() {
        return contest;
    }

    public void setContest(Contest contest) {
        this.contest = contest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Double totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getProblemsSolved() {
        return problemsSolved;
    }

    public void setProblemsSolved(Integer problemsSolved) {
        this.problemsSolved = problemsSolved;
    }

    public Long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Long totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public List<ProblemScore> getProblemScores() {
        return problemScores;
    }

    public void setProblemScores(List<ProblemScore> problemScores) {
        this.problemScores = problemScores != null ? problemScores : new ArrayList<>();
    }

    public MedalType getMedal() {
        return medal;
    }

    public void setMedal(MedalType medal) {
        this.medal = medal;
    }

    public LocalDateTime getLastSubmissionTime() {
        return lastSubmissionTime;
    }

    public void setLastSubmissionTime(LocalDateTime lastSubmissionTime) {
        this.lastSubmissionTime = lastSubmissionTime;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    @Override
    public String toString() {
        return "ContestLeaderboardEntry{id=" + id + ", rank=" + rank + 
               ", totalPoints=" + totalPoints + ", problemsSolved=" + problemsSolved + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContestLeaderboardEntry that = (ContestLeaderboardEntry) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
