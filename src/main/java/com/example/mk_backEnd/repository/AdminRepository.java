package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminRepository extends JpaRepository<Admin, String> {

    long countByWorkspaceId(String workspaceId);

    List<Admin> findByWorkspaceId(String workspaceId);
}
