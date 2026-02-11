package com.codearena.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;

import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for UserService.
 * 
 * <p>Tests the following correctness properties:
 * <ul>
 *   <li>Property 8: Role Update Persistence</li>
 * </ul>
 */
public class UserServiceProperties {

    private UserRepository userRepository;
    private UserService userService;
    private AdminAuditService adminAuditService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        adminAuditService = Mockito.mock(AdminAuditService.class);
        userService = new UserServiceImpl(userRepository, adminAuditService);
    }

    /**
     * Property 8: Role Update Persistence
     * 
     * For any admin-initiated role change, the new role SHALL be persisted
     * and enforced on all subsequent requests by that user.
     * 
     * Validates: Requirements 3.5, 14.1
     */
    @Property(tries = 100)
    @Tag("Feature: codearena-platform, Property 8: Role Update Persistence")
    void roleUpdatePersistence(@ForAll("users") User targetUser,
                                @ForAll("adminUsers") User admin,
                                @ForAll("roles") Role newRole) {
        Long targetUserId = targetUser.getId();
        
        // Setup mock
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Execute
        userService.updateRole(targetUserId, newRole, admin);
        
        // Verify: the role was updated and persisted
        assertThat(targetUser.getRole()).isEqualTo(newRole);
        verify(userRepository).save(targetUser);
    }

    /**
     * Property 8 (continued): Non-admin role update rejection
     * 
     * Non-admin users cannot update roles.
     */
    @Property(tries = 100)
    @Tag("Feature: codearena-platform, Property 8: Role Update Persistence - Non-Admin Rejection")
    void nonAdminRoleUpdateRejection(@ForAll("users") User targetUser,
                                      @ForAll("nonAdminUsers") User requester,
                                      @ForAll("roles") Role newRole) {
        Long targetUserId = targetUser.getId();
        
        // Execute and verify: non-admin cannot update roles
        assertThatThrownBy(() -> userService.updateRole(targetUserId, newRole, requester))
            .isInstanceOf(AccessDeniedException.class);
        
        // Verify: no save was attempted
        verify(userRepository, never()).save(any());
    }

    // Arbitraries

    @Provide
    Arbitrary<User> users() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 100),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> s + "@example.com"),
            Arbitraries.of(Role.class)
        ).as((id, name, email, role) -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setRole(role);
            return user;
        });
    }

    @Provide
    Arbitrary<User> adminUsers() {
        return Combinators.combine(
            Arbitraries.longs().between(1000, 2000),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> s + "@admin.com")
        ).as((id, name, email) -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setRole(Role.ADMIN);
            return user;
        });
    }

    @Provide
    Arbitrary<User> nonAdminUsers() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 100),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> s + "@example.com"),
            Arbitraries.of(Role.USER, Role.PROBLEM_SETTER)
        ).as((id, name, email, role) -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setRole(role);
            return user;
        });
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.class);
    }
}
