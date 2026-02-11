package com.codearena.dto;

import java.time.LocalDateTime;

import com.codearena.entity.Role;
import com.codearena.entity.User;

public record UserListDto(Long id, String name, String email, Role role, LocalDateTime createdAt, int problemsSolved) {
    public static UserListDto fromEntity(User user, int problemsSolved) {
        return new UserListDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(), problemsSolved);
    }
}
