package com.codearena.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "contest_registrations",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_contest_user_registration",
           columnNames = {"contest_id", "user_id"}
       ))
public class ContestRegistration {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "participant_start_time")
    private LocalDateTime participantStartTime;

    @Column(name = "first_submission_time")
    private LocalDateTime firstSubmissionTime;

    @Column(name = "last_submission_time")
    private LocalDateTime lastSubmissionTime;

    @Column(name = "auto_submit_processed", nullable = false)
    private Boolean autoSubmitProcessed = false;

    public ContestRegistration() {
    }

    public ContestRegistration(Contest contest, User user) {
        this.contest = contest;
        this.user = user;
    }

    public boolean hasStarted() {
        return participantStartTime != null;
    }

    public LocalDateTime getEffectiveEndTime() {
        if (contest.getTimerMode() == TimerMode.GLOBAL) {
            return contest.getEndTime();
        }
        if (participantStartTime == null) {
            return null;
        }
        LocalDateTime individualEnd = participantStartTime.plusMinutes(contest.getDurationMinutes());
        return individualEnd.isBefore(contest.getEndTime()) ? individualEnd : contest.getEndTime();
    }

    public long getRemainingTimeSeconds() {
        LocalDateTime effectiveEnd = getEffectiveEndTime();
        if (effectiveEnd == null) {
            return 0;
        }
        long remaining = Duration.between(LocalDateTime.now(), effectiveEnd).getSeconds();
        return Math.max(0, remaining);
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

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public LocalDateTime getParticipantStartTime() {
        return participantStartTime;
    }

    public void setParticipantStartTime(LocalDateTime participantStartTime) {
        this.participantStartTime = participantStartTime;
    }

    public LocalDateTime getFirstSubmissionTime() {
        return firstSubmissionTime;
    }

    public void setFirstSubmissionTime(LocalDateTime firstSubmissionTime) {
        this.firstSubmissionTime = firstSubmissionTime;
    }

    public LocalDateTime getLastSubmissionTime() {
        return lastSubmissionTime;
    }

    public void setLastSubmissionTime(LocalDateTime lastSubmissionTime) {
        this.lastSubmissionTime = lastSubmissionTime;
    }

    public Boolean getAutoSubmitProcessed() {
        return autoSubmitProcessed;
    }

    public void setAutoSubmitProcessed(Boolean autoSubmitProcessed) {
        this.autoSubmitProcessed = autoSubmitProcessed;
    }

    public boolean isAutoSubmitProcessed() {
        return autoSubmitProcessed != null && autoSubmitProcessed;
    }

    @Override
    public String toString() {
        return "ContestRegistration{id=" + id + ", status=" + status + 
               ", registeredAt=" + registeredAt + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContestRegistration that = (ContestRegistration) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
