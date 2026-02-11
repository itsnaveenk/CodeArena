package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.PlatformStatsDto;
import com.codearena.dto.ProblemManagementDto;
import com.codearena.dto.UserListDto;
import com.codearena.entity.Difficulty;
import com.codearena.entity.ProblemStatus;
import com.codearena.entity.Role;
import com.codearena.entity.User;

public interface AdminService {

    PlatformStatsDto getPlatformStats();

    Page<UserListDto> listUsers(Role role, String search, Pageable pageable);

    Page<ProblemManagementDto> listAllProblems(
        ProblemStatus status,
        Difficulty difficulty,
        String search,
        Pageable pageable
    );

    int bulkPublish(java.util.List<Long> problemIds, User admin);

    int bulkReject(java.util.List<Long> problemIds, User admin);

    int bulkArchive(java.util.List<Long> problemIds, User admin);
}
