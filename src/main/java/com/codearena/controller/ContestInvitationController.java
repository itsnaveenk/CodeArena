package com.codearena.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.ContestRegistrationDto;
import com.codearena.entity.ContestInvitation;
import com.codearena.entity.User;
import com.codearena.service.ContestInvitationService;
import com.codearena.service.ContestRegistrationService;

@RestController
@RequestMapping("/api/contests/invitations")
public class ContestInvitationController {

    private final ContestInvitationService invitationService;
    private final ContestRegistrationService registrationService;

    public ContestInvitationController(
            ContestInvitationService invitationService,
            ContestRegistrationService registrationService) {
        this.invitationService = invitationService;
        this.registrationService = registrationService;
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<ContestRegistrationDto> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal User user) {
        invitationService.acceptInvitation(token, user);
        
        ContestInvitation invitation = invitationService.getInvitationByToken(token);
        
        ContestRegistrationDto dto = registrationService.getRegistration(
            invitation.getContest().getId(), user);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{token}/decline")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal User user) {
        invitationService.declineInvitation(token, user);
        return ResponseEntity.noContent().build();
    }
}
