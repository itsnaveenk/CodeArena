package com.codearena.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codearena.dto.UserDto;
import com.codearena.entity.AdminAuditAction;
import com.codearena.entity.AdminAuditTargetType;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    public UserServiceImpl(UserRepository userRepository, AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.adminAuditService = adminAuditService;
    }

    @Override
    public UserDto getCurrentUser(User user) {
        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public void updateRole(Long userId, Role newRole, User admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can update user roles");
        }

        if (admin.getId().equals(userId)) {
            throw new AccessDeniedException("Cannot change your own role");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        adminAuditService.record(
            admin,
            AdminAuditAction.USER_ROLE_UPDATED,
            AdminAuditTargetType.USER,
            String.valueOf(userId),
            Map.of(
                "targetEmail", user.getEmail(),
                "oldRole", oldRole.name(),
                "newRole", newRole.name()
            )
        );

        logger.info("Admin {} updated user {} role from {} to {}",
            admin.getEmail(), user.getEmail(), oldRole, newRole);
    }
}
