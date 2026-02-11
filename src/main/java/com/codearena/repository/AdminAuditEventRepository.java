package com.codearena.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codearena.entity.AdminAuditAction;
import com.codearena.entity.AdminAuditEvent;

@Repository
public interface AdminAuditEventRepository extends JpaRepository<AdminAuditEvent, Long> {

    Page<AdminAuditEvent> findByAction(AdminAuditAction action, Pageable pageable);

    Page<AdminAuditEvent> findByActorUserId(Long actorUserId, Pageable pageable);

    Page<AdminAuditEvent> findByActorUserIdAndAction(Long actorUserId, AdminAuditAction action, Pageable pageable);
}

