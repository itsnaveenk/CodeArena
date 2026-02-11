package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.AdminAuditAction;
import com.codearena.entity.AdminAuditEvent;
import com.codearena.entity.AdminAuditTargetType;

public record AdminAuditEventDto(
    Long id,
    Long actorUserId,
    String actorEmail,
    AdminAuditAction action,
    AdminAuditTargetType targetType,
    String targetId,
    String metadataJson,
    LocalDateTime createdAt
) {
    public static AdminAuditEventDto fromEntity(AdminAuditEvent e) {
        return new AdminAuditEventDto(e.getId(), e.getActorUserId(), e.getActorEmail(), e.getAction(), e.getTargetType(), e.getTargetId(), e.getMetadataJson(), e.getCreatedAt());
    }
}
