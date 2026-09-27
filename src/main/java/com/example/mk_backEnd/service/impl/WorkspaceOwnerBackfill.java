package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.repository.JobAdRepository;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills in two things that older rows don't have, so several workspaces per admin work on top of
 * existing data:
 * <ul>
 *   <li>Workspaces created before ownership was tracked have no owner. The first admin who joined
 *       each of them is the one who signed the business up, so they become the owner.</li>
 *   <li>Job posts created before jobs were tied to a workspace get their creator's workspace.</li>
 * </ul>
 * Safe to run on every start: it only touches rows that are still missing the value.
 */
@Component
public class WorkspaceOwnerBackfill {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceOwnerBackfill.class);

    private final WorkspaceRepository workspaceRepository;
    private final AdminRepository adminRepository;
    private final JobAdRepository jobAdRepository;

    public WorkspaceOwnerBackfill(WorkspaceRepository workspaceRepository, AdminRepository adminRepository,
                                  JobAdRepository jobAdRepository) {
        this.workspaceRepository = workspaceRepository;
        this.adminRepository = adminRepository;
        this.jobAdRepository = jobAdRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        int owners = 0;
        for (Workspace workspace : workspaceRepository.findByOwnerAdminIdIsNull()) {
            var firstAdmin = adminRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspace.getId()).stream().findFirst();
            if (firstAdmin.isPresent()) {
                workspace.setOwnerAdminId(firstAdmin.get().getId());
                workspaceRepository.save(workspace);
                owners++;
            }
        }

        int jobs = 0;
        for (JobAd job : jobAdRepository.findByWorkspaceIsNull()) {
            if (job.getCreatedBy() != null) {
                job.setWorkspace(job.getCreatedBy().getWorkspace());
                jobAdRepository.save(job);
                jobs++;
            }
        }

        if (owners > 0 || jobs > 0) {
            log.info("Backfilled {} workspace owner(s) and {} job workspace(s).", owners, jobs);
        }
    }
}
