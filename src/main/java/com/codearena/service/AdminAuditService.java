package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.AdminAuditEventDto;
import com.codearena.entity.AdminAuditAction;
import com.codearena.entity.AdminAuditTargetType;
import com.codearena.entity.User;

public interface AdminAuditService {

    void record(User actor, AdminAuditAction action, AdminAuditTargetType targetType, String targetId, Object metadata);

    Page<AdminAuditEventDto> list(AdminAuditAction action, Long actorUserId, Pageable pageable);
}
