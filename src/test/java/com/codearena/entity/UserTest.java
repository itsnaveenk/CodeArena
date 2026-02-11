package com.codearena.entity;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Unit tests for the User entity.
 */
class UserTest {

    @Test
    @DisplayName("Default constructor should create user with default role USER")
    void defaultConstructorCreatesUserWithDefaultRole() {
        User user = new User();
        assertEquals(Role.USER, user.getRole());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields correctly")
    void parameterizedConstructorSetsAllFields() {
        String name = "John Doe";
        String email = "john@example.com";
        String passwordHash = "$2a$10$hashedpassword";
        Role role = Role.PROBLEM_SETTER;

        User user = new User(name, email, passwordHash, role);

        assertEquals(name, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());
        assertEquals(role, user.getRole());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("getUsername should return email")
    void getUsernameReturnsEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        
        assertEquals("test@example.com", user.getUsername());
    }

    @Test
    @DisplayName("getPassword should return passwordHash")
    void getPasswordReturnsPasswordHash() {
        User user = new User();
        String hash = "$2a$10$hashedpassword";
        user.setPasswordHash(hash);
        
        assertEquals(hash, user.getPassword());
    }

    @Test
    @DisplayName("getAuthorities should return role with ROLE_ prefix for USER")
    void getAuthoritiesReturnsRoleWithPrefixForUser() {
        User user = new User();
        user.setRole(Role.USER);
        
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("getAuthorities should return role with ROLE_ prefix for PROBLEM_SETTER")
    void getAuthoritiesReturnsRoleWithPrefixForProblemSetter() {
        User user = new User();
        user.setRole(Role.PROBLEM_SETTER);
        
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROBLEM_SETTER")));
    }

    @Test
    @DisplayName("getAuthorities should return role with ROLE_ prefix for ADMIN")
    void getAuthoritiesReturnsRoleWithPrefixForAdmin() {
        User user = new User();
        user.setRole(Role.ADMIN);
        
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("UserDetails boolean methods should return true")
    void userDetailsMethodsReturnTrue() {
        User user = new User();
        
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Setters and getters should work correctly")
    void settersAndGettersWorkCorrectly() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash123");
        user.setRole(Role.ADMIN);
        user.setCreatedAt(now);
        
        assertEquals(1L, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    @DisplayName("equals should return true for same id")
    void equalsReturnsTrueForSameId() {
        User user1 = new User();
        user1.setId(1L);
        
        User user2 = new User();
        user2.setId(1L);
        
        assertEquals(user1, user2);
    }

    @Test
    @DisplayName("equals should return false for different id")
    void equalsReturnsFalseForDifferentId() {
        User user1 = new User();
        user1.setId(1L);
        
        User user2 = new User();
        user2.setId(2L);
        
        assertNotEquals(user1, user2);
    }

    @Test
    @DisplayName("equals should return false for null id")
    void equalsReturnsFalseForNullId() {
        User user1 = new User();
        user1.setId(null);
        
        User user2 = new User();
        user2.setId(1L);
        
        assertNotEquals(user1, user2);
    }

    @Test
    @DisplayName("equals should return false for null object")
    void equalsReturnsFalseForNull() {
        User user = new User();
        user.setId(1L);
        
        assertNotEquals(null, user);
    }

    @Test
    @DisplayName("equals should return true for same object")
    void equalsReturnsTrueForSameObject() {
        User user = new User();
        user.setId(1L);
        
        assertEquals(user, user);
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeIsConsistent() {
        User user = new User();
        user.setId(1L);
        
        int hash1 = user.hashCode();
        int hash2 = user.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("toString should contain user information")
    void toStringContainsUserInfo() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        
        String str = user.toString();
        
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("name='Test User'"));
        assertTrue(str.contains("email='test@example.com'"));
        assertTrue(str.contains("role=USER"));
    }
}
