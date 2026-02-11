package com.codearena.repository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.codearena.entity.Role;
import com.codearena.entity.User;

/**
 * Repository tests for UserRepository using H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "$2a$10$hashedpassword", Role.USER);
        entityManager.persistAndFlush(testUser);
    }

    @Test
    @DisplayName("findByEmail should return user when email exists")
    void findByEmailReturnsUserWhenEmailExists() {
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals(Role.USER, found.get().getRole());
    }

    @Test
    @DisplayName("findByEmail should return empty when email does not exist")
    void findByEmailReturnsEmptyWhenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findByEmail should be case-sensitive")
    void findByEmailIsCaseSensitive() {
        Optional<User> found = userRepository.findByEmail("TEST@EXAMPLE.COM");
        
        // Email lookup should be case-sensitive by default
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("existsByEmail should return true when email exists")
    void existsByEmailReturnsTrueWhenEmailExists() {
        boolean exists = userRepository.existsByEmail("test@example.com");
        
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByEmail should return false when email does not exist")
    void existsByEmailReturnsFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");
        
        assertFalse(exists);
    }

    @Test
    @DisplayName("save should persist new user")
    void savePersistsNewUser() {
        User newUser = new User("New User", "new@example.com", "$2a$10$newhash", Role.PROBLEM_SETTER);
        
        User saved = userRepository.save(newUser);
        
        assertNotNull(saved.getId());
        assertEquals("New User", saved.getName());
        assertEquals("new@example.com", saved.getEmail());
        assertEquals(Role.PROBLEM_SETTER, saved.getRole());
    }

    @Test
    @DisplayName("save should update existing user")
    void saveUpdatesExistingUser() {
        testUser.setName("Updated Name");
        testUser.setRole(Role.ADMIN);
        
        User updated = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();
        
        User found = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Updated Name", found.getName());
        assertEquals(Role.ADMIN, found.getRole());
    }

    @Test
    @DisplayName("findById should return user when id exists")
    void findByIdReturnsUserWhenIdExists() {
        Optional<User> found = userRepository.findById(testUser.getId());
        
        assertTrue(found.isPresent());
        assertEquals(testUser.getEmail(), found.get().getEmail());
    }

    @Test
    @DisplayName("findById should return empty when id does not exist")
    void findByIdReturnsEmptyWhenIdDoesNotExist() {
        Optional<User> found = userRepository.findById(99999L);
        
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("delete should remove user")
    void deleteRemovesUser() {
        Long userId = testUser.getId();
        
        userRepository.delete(testUser);
        entityManager.flush();
        
        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("count should return correct number of users")
    void countReturnsCorrectNumber() {
        long initialCount = userRepository.count();
        
        User anotherUser = new User("Another User", "another@example.com", "$2a$10$hash", Role.USER);
        entityManager.persistAndFlush(anotherUser);
        
        assertEquals(initialCount + 1, userRepository.count());
    }

    @Test
    @DisplayName("User should have createdAt set automatically")
    void userHasCreatedAtSetAutomatically() {
        User newUser = new User("New User", "new2@example.com", "$2a$10$hash", Role.USER);
        User saved = userRepository.save(newUser);
        entityManager.flush();
        
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Multiple users with different roles can be saved")
    void multipleUsersWithDifferentRolesCanBeSaved() {
        User admin = new User("Admin User", "admin@example.com", "$2a$10$hash", Role.ADMIN);
        User setter = new User("Setter User", "setter@example.com", "$2a$10$hash", Role.PROBLEM_SETTER);
        
        userRepository.save(admin);
        userRepository.save(setter);
        entityManager.flush();
        
        assertEquals(Role.ADMIN, userRepository.findByEmail("admin@example.com").get().getRole());
        assertEquals(Role.PROBLEM_SETTER, userRepository.findByEmail("setter@example.com").get().getRole());
    }
}
