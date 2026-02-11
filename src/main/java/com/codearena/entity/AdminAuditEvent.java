package com.codearena.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "admin_audit_events",
    indexes = {
        @Index(name = "idx_audit_created_at", columnList = "createdAt"),
        @Index(name = "idx_audit_actor_id", columnList = "actorUserId"),
        @Index(name = "idx_audit_action", columnList = "action")
    }
)
public class AdminAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorUserId;

    private String actorEmail;

    @Enumerated(EnumType.STRING)
    private AdminAuditAction action;

    @Enumerated(EnumType.STRING)
    private AdminAuditTargetType targetType;

    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AdminAuditEvent() {
    }

    public AdminAuditEvent(Long actorUserId, String actorEmail, AdminAuditAction action,
                           AdminAuditTargetType targetType, String targetId, String metadataJson) {
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadataJson = metadataJson;
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public AdminAuditAction getAction() {
        return action;
    }

    public void setAction(AdminAuditAction action) {
        this.action = action;
    }

    public AdminAuditTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(AdminAuditTargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
