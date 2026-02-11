package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.Role;
import com.codearena.entity.User;

public record UserDto(Long id, String name, String email, Role role, LocalDateTime createdAt) {
    public static UserDto fromEntity(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
