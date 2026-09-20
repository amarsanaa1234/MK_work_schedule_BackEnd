package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Flat, admin-dashboard-friendly view of a {@link com.example.mk_backEnd.domain.JobAd}.
 * Used instead of serializing the entity directly so the leader/crew come back as
 * plain names (JobAd.crew is @JsonIgnore) and the response doesn't leak the leader's
 * whole User/Workspace graph.
 */
@Getter
@AllArgsConstructor
public class JobAdSummaryResponse {

    private String id;
    private String title;
    private String jobType;
    private String truck;
    private String notes;
    private String status;
    private LocalDate workDate;
    private LocalTime startTime;
    private String addressLine;
    private int requiredCount;
    private EmployeeSummaryResponse leader;
    private List<EmployeeSummaryResponse> crew;
    private LocalDateTime createdAt;
}
