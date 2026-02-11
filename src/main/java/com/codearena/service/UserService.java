package com.codearena.service;

import com.codearena.dto.UserDto;
import com.codearena.entity.Role;
import com.codearena.entity.User;

public interface UserService {

    UserDto getCurrentUser(User user);

    void updateRole(Long userId, Role newRole, User admin);
}
