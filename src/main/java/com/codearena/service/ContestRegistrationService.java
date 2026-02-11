package com.codearena.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.ContestRegistrationDto;
import com.codearena.dto.ParticipationStatus;
import com.codearena.entity.ContestRegistration;
import com.codearena.entity.User;

public interface ContestRegistrationService {


    ContestRegistration register(Long contestId, User user);

    void withdraw(Long contestId, User user);

    void startContest(Long contestId, User user);


    ContestRegistrationDto getRegistration(Long contestId, User user);

    Page<ContestRegistrationDto> getContestRegistrations(Long contestId, Pageable pageable);

    boolean isRegistered(Long contestId, Long userId);

    int getRegisteredCount(Long contestId);


    ParticipationStatus checkParticipationStatus(Long contestId, User user);
}
