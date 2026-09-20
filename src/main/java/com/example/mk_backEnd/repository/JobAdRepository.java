package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.JobAd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JobAdRepository extends JpaRepository<JobAd, String> {

    List<JobAd> findByCreatedById(String adminId);

    List<JobAd> findByLeaderId(String leaderId);

    List<JobAd> findByWorkDateBetween(LocalDate from, LocalDate to);

    List<JobAd> findByCreatedBy_Workspace_IdAndWorkDateBetween(String workspaceId, LocalDate from, LocalDate to);
}
