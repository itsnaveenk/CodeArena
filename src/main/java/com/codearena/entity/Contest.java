package com.codearena.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "contests")
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestStatus status = ContestStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestVisibility visibility = ContestVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestCategory category = ContestCategory.PRACTICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "timer_mode", nullable = false, length = 20)
    private TimerMode timerMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_model", nullable = false, length = 20)
    private ScoringModel scoringModel = ScoringModel.PARTIAL;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "registration_start_time")
    private LocalDateTime registrationStartTime;

    @Column(name = "registration_end_time")
    private LocalDateTime registrationEndTime;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "leaderboard_freeze_minutes", nullable = false)
    private Integer leaderboardFreezeMinutes = 0;

    @Column(name = "leaderboard_delay_seconds", nullable = false)
    private Integer leaderboardDelaySeconds = 0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ContestProblem> contestProblems = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContestRegistration> registrations = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContestInvitation> invitations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (registrationStartTime == null) {
            registrationStartTime = createdAt;
        }
        if (registrationEndTime == null) {
            registrationEndTime = startTime;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Contest() {
    }

    public Contest(String title, String slug, String description, TimerMode timerMode,
                   LocalDateTime startTime, LocalDateTime endTime, User createdBy) {
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.timerMode = timerMode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdBy = createdBy;
    }

    public int getTotalPossiblePoints() {
        return contestProblems.stream()
            .mapToInt(ContestProblem::getPointValue)
            .sum();
    }

    public int getRegisteredCount() {
        return (int) registrations.stream()
            .filter(r -> r.getStatus() == RegistrationStatus.REGISTERED)
            .count();
    }

    public boolean isRegistrationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return status == ContestStatus.PUBLISHED
            && now.isAfter(registrationStartTime)
            && now.isBefore(registrationEndTime);
    }

    public boolean canModifyProblems() {
        return status == ContestStatus.DRAFT || status == ContestStatus.PUBLISHED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContestStatus getStatus() {
        return status;
    }

    public void setStatus(ContestStatus status) {
        this.status = status;
    }

    public ContestVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ContestVisibility visibility) {
        this.visibility = visibility;
    }

    public ContestCategory getCategory() {
        return category;
    }

    public void setCategory(ContestCategory category) {
        this.category = category;
    }

    public TimerMode getTimerMode() {
        return timerMode;
    }

    public void setTimerMode(TimerMode timerMode) {
        this.timerMode = timerMode;
    }

    public ScoringModel getScoringModel() {
        return scoringModel;
    }

    public void setScoringModel(ScoringModel scoringModel) {
        this.scoringModel = scoringModel;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getRegistrationStartTime() {
        return registrationStartTime;
    }

    public void setRegistrationStartTime(LocalDateTime registrationStartTime) {
        this.registrationStartTime = registrationStartTime;
    }

    public LocalDateTime getRegistrationEndTime() {
        return registrationEndTime;
    }

    public void setRegistrationEndTime(LocalDateTime registrationEndTime) {
        this.registrationEndTime = registrationEndTime;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getLeaderboardFreezeMinutes() {
        return leaderboardFreezeMinutes;
    }

    public void setLeaderboardFreezeMinutes(Integer leaderboardFreezeMinutes) {
        this.leaderboardFreezeMinutes = leaderboardFreezeMinutes;
    }

    public Integer getLeaderboardDelaySeconds() {
        return leaderboardDelaySeconds;
    }

    public void setLeaderboardDelaySeconds(Integer leaderboardDelaySeconds) {
        this.leaderboardDelaySeconds = leaderboardDelaySeconds;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ContestProblem> getContestProblems() {
        return contestProblems;
    }

    public void setContestProblems(List<ContestProblem> contestProblems) {
        this.contestProblems = contestProblems != null ? contestProblems : new ArrayList<>();
    }

    public List<ContestRegistration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(List<ContestRegistration> registrations) {
        this.registrations = registrations != null ? registrations : new ArrayList<>();
    }

    public List<ContestInvitation> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<ContestInvitation> invitations) {
        this.invitations = invitations != null ? invitations : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Contest{id=" + id + ", title='" + title + "', slug='" + slug + 
               "', status=" + status + ", timerMode=" + timerMode + 
               ", startTime=" + startTime + ", endTime=" + endTime + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contest contest = (Contest) o;
        return id != null && Objects.equals(id, contest.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
