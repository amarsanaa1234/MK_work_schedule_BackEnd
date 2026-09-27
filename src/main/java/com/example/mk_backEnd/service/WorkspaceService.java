package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.AddWorkspaceRequest;
import com.example.mk_backEnd.dto.CreateWorkspaceRequest;

import java.util.List;

public interface WorkspaceService {

    record WorkspaceAndAdmin(Workspace workspace, Admin admin) {
    }

    WorkspaceAndAdmin createWorkspace(CreateWorkspaceRequest request);

    Workspace findByOrganizationId(String organizationId);

    Workspace findByUserId(String userId);

    /**
     * Every workspace this admin can open: the ones they own (oldest first) plus the one they are
     * currently in. A single entry for everyone who isn't a Business owner.
     */
    List<Workspace> workspacesOf(Admin admin);

    /** How many workspaces this user can open - 1 for crew and for admins with just one. */
    int workspaceCountFor(User user);

    /**
     * Adds another workspace under the admin's ownership, with its own fresh Org ID so it has its
     * own people. Needs the Business plan and room in its workspace quota.
     */
    Workspace addWorkspace(String adminId, AddWorkspaceRequest request);

    /** Makes one of the admin's workspaces the active one, so every screen and API call follows it. */
    Admin switchWorkspace(String adminId, String organizationId);
}
