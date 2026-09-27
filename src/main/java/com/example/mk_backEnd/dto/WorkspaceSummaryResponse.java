package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One workspace an admin can open, for the workspace picker and the admin profile. */
@Getter
@AllArgsConstructor
public class WorkspaceSummaryResponse {

    private String organizationId;
    private String businessName;
    private String address;
    private String industry;
    private long peopleCount;
    /** This is the workspace the admin currently has open. */
    private boolean active;
    /** Crew with money owed for the requested pay period; null when no period was requested. */
    private Integer unpaidCount;
    /** Missing hour logs in the requested pay period; null when no period was requested. */
    private Integer missingLogCount;
}
