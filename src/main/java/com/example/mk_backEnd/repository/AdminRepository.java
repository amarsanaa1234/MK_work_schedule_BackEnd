package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminRepository extends JpaRepository<Admin, String> {

    long countByWorkspaceId(String workspaceId);

    List<Admin> findByWorkspaceId(String workspaceId);

    List<Admin> findByWorkspaceIdOrderByCreatedAtAsc(String workspaceId);

    /**
     * Every admin of a workspace: the ones whose active workspace it currently is, plus its owner
     * even while the owner has switched to one of their other workspaces. Use this (not
     * {@link #findByWorkspaceId}) anywhere people are listed or counted.
     */
    @Query("select a from Admin a where a.workspace.id = :workspaceId "
            + "or a.id = (select w.ownerAdminId from Workspace w where w.id = :workspaceId)")
    List<Admin> findMembers(@Param("workspaceId") String workspaceId);

    @Query("select count(a) from Admin a where a.workspace.id = :workspaceId "
            + "or a.id = (select w.ownerAdminId from Workspace w where w.id = :workspaceId)")
    long countMembers(@Param("workspaceId") String workspaceId);
}
