package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {

    Optional<Workspace> findByOrganizationId(String organizationId);

    boolean existsByOrganizationId(String organizationId);

    Optional<Workspace> findByStripeSubscriptionId(String stripeSubscriptionId);

    /** Workspaces one admin owns, oldest first (the first is the one they originally signed up with). */
    List<Workspace> findByOwnerAdminIdOrderByCreatedAtAsc(String ownerAdminId);

    long countByOwnerAdminId(String ownerAdminId);

    List<Workspace> findByOwnerAdminIdIsNull();
}
