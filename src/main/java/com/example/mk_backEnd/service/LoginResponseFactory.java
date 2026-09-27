package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.LoginResponse;
import org.springframework.stereotype.Component;

/** Builds the session the app keeps after sign-in, register, workspace setup or a workspace switch. */
@Component
public class LoginResponseFactory {

    private final WorkspaceService workspaceService;

    public LoginResponseFactory(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public LoginResponse build(User user, String userType, String token) {
        Workspace workspace = user.getWorkspace();
        return new LoginResponse(
                true,
                user.getId(),
                userType,
                user.getFullName(),
                token,
                workspace.getOrganizationId(),
                user.getPhotoUrl(),
                workspace.getIndustry(),
                workspace.getAddress(),
                workspace.getBusinessName(),
                workspaceService.workspaceCountFor(user));
    }
}
