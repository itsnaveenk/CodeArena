package com.codearena.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.codearena.dto.BulkInviteResult;
import com.codearena.dto.ContestInvitationDto;
import com.codearena.entity.ContestInvitation;
import com.codearena.entity.User;

public interface ContestInvitationService {


    ContestInvitation invite(Long contestId, String email, User inviter);

    List<BulkInviteResult> bulkInvite(Long contestId, List<String> emails, User inviter);


    void acceptInvitation(String token, User user);

    void declineInvitation(String token, User user);

    void revokeInvitation(Long invitationId, User revoker);


    Page<ContestInvitationDto> getContestInvitations(Long contestId, Pageable pageable);

    ContestInvitation getInvitationByToken(String token);
}
