package com.codearena.dto;

import com.codearena.entity.User;

public record UserSummaryDto(
    Long id,
    String name
) {
    public static UserSummaryDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDto(user.getId(), user.getName());
    }
}
