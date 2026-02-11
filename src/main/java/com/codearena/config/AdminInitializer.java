package com.codearena.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.UserRepository;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@example.com").isEmpty()) {
                User admin = new User("Admin User", "admin@example.com", passwordEncoder.encode("password123"),
                        Role.ADMIN);
                userRepository.save(admin);
                System.out.println("ADMIN_INITIALIZER: Admin user created: admin@example.com");
            } else {
                User admin = userRepository.findByEmail("admin@example.com").get();
                if (admin.getRole() != Role.ADMIN) {
                    admin.setRole(Role.ADMIN);
                    userRepository.save(admin);
                    System.out.println("ADMIN_INITIALIZER: Updated existing user to ADMIN role: admin@example.com");
                }
            }
        };
    }
}
