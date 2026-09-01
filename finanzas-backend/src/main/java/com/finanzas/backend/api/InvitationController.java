package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.service.WorkspaceCollaborationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private final WorkspaceCollaborationService collaboration;

    public InvitationController(WorkspaceCollaborationService collaboration) {
        this.collaboration = collaboration;
    }

    @GetMapping("/mine")
    public List<HouseholdDtos.InvitationResponse> mine(Authentication authentication) {
        return collaboration.listMyPendingInvitations(CurrentUser.id(authentication));
    }

    @PostMapping("/{invitationId}/accept")
    public HouseholdDtos.InvitationResponse accept(Authentication authentication, @PathVariable UUID invitationId) {
        return collaboration.acceptInvitation(CurrentUser.id(authentication), invitationId);
    }

    @PostMapping("/{invitationId}/reject")
    public HouseholdDtos.InvitationResponse reject(Authentication authentication, @PathVariable UUID invitationId) {
        return collaboration.rejectInvitation(CurrentUser.id(authentication), invitationId);
    }
}
