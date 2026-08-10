package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {

    Optional<Workspace> findByOrganizationId(String organizationId);

    boolean existsByOrganizationId(String organizationId);
}
