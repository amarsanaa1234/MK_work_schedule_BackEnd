package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.JobAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JobAdRepository extends JpaRepository<JobAd, String> {

    List<JobAd> findByCreatedById(String adminId);

    List<JobAd> findByLeaderId(String leaderId);

    List<JobAd> findByWorkDateBetween(LocalDate from, LocalDate to);

    /** Jobs that have no workspace recorded yet (created before jobs were tied to a workspace). */
    List<JobAd> findByWorkspaceIsNull();

    /**
     * A workspace's jobs in [from, to]. Jobs that still have no workspace recorded (posted by an
     * older build of the backend) count as belonging to their creator's current workspace.
     */
    @Query("select j from JobAd j where j.workDate between :from and :to and "
            + "(j.workspace.id = :workspaceId or (j.workspace is null and j.createdBy.workspace.id = :workspaceId))")
    List<JobAd> findForWorkspace(@Param("workspaceId") String workspaceId,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);
}
