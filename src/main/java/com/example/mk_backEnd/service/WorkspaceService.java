package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.CreateWorkspaceRequest;

public interface WorkspaceService {

    record WorkspaceAndAdmin(Workspace workspace, Admin admin) {
    }

    WorkspaceAndAdmin createWorkspace(CreateWorkspaceRequest request);

    Workspace findByOrganizationId(String organizationId);
}
