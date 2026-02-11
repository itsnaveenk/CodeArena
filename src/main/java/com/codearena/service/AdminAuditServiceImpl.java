package com.codearena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.AdminAuditEventDto;
import com.codearena.entity.AdminAuditAction;
import com.codearena.entity.AdminAuditEvent;
import com.codearena.entity.AdminAuditTargetType;
import com.codearena.entity.User;
import com.codearena.repository.AdminAuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditServiceImpl.class);

    private final AdminAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AdminAuditServiceImpl(AdminAuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, AdminAuditAction action, AdminAuditTargetType targetType, String targetId, Object metadata) {
        try {
            String metadataJson = metadata == null ? null : toJson(metadata);
            AdminAuditEvent event = new AdminAuditEvent(
                actor != null ? actor.getId() : null,
                actor != null ? actor.getEmail() : null,
                action,
                targetType,
                targetId,
                metadataJson
            );
            repository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record admin audit event {}: {}", action, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAuditEventDto> list(AdminAuditAction action, Long actorUserId, Pageable pageable) {
        if (action != null && actorUserId != null) {
            return repository.findByActorUserIdAndAction(actorUserId, action, pageable).map(AdminAuditEventDto::fromEntity);
        }
        if (action != null) {
            return repository.findByAction(action, pageable).map(AdminAuditEventDto::fromEntity);
        }
        if (actorUserId != null) {
            return repository.findByActorUserId(actorUserId, pageable).map(AdminAuditEventDto::fromEntity);
        }
        return repository.findAll(pageable).map(AdminAuditEventDto::fromEntity);
    }

    private String toJson(Object metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "\"<unserializable>\"";
        }
    }
}
