package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.service.WorkspaceCollaborationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
public class WorkspaceMemberController {
    private final WorkspaceCollaborationService collaboration;

    public WorkspaceMemberController(WorkspaceCollaborationService collaboration) {
        this.collaboration = collaboration;
    }

    @GetMapping("/members")
    public List<HouseholdDtos.MemberResponse> members(Authentication authentication, @PathVariable UUID workspaceId) {
        return collaboration.listMembers(CurrentUser.id(authentication), workspaceId);
    }

    @PatchMapping("/members/{memberId}")
    public HouseholdDtos.MemberResponse changeRole(Authentication authentication,
                                                   @PathVariable UUID workspaceId,
                                                   @PathVariable UUID memberId,
                                                   @Valid @RequestBody HouseholdDtos.MemberRoleRequest request) {
        return collaboration.changeRole(CurrentUser.id(authentication), workspaceId, memberId, request);
    }

    @DeleteMapping("/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID memberId) {
        collaboration.removeMember(CurrentUser.id(authentication), workspaceId, memberId);
    }

    @GetMapping("/invitations")
    public List<HouseholdDtos.InvitationResponse> invitations(Authentication authentication, @PathVariable UUID workspaceId) {
        return collaboration.listWorkspaceInvitations(CurrentUser.id(authentication), workspaceId);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdDtos.InvitationResponse invite(Authentication authentication,
                                                  @PathVariable UUID workspaceId,
                                                  @Valid @RequestBody HouseholdDtos.InvitationRequest request) {
        return collaboration.invite(CurrentUser.id(authentication), workspaceId, request);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID invitationId) {
        collaboration.cancelInvitation(CurrentUser.id(authentication), workspaceId, invitationId);
    }
}
