package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One person in the admin's workspace for the Employees screen — hours, pay and paid
 * status for a single pay period. Admins only carry the identity/contact fields.
 */
@Getter
@AllArgsConstructor
public class EmployeeOverviewResponse {

    private String id;
    private String fullName;
    /** "Crew" or "Admin". */
    private String role;
    private String email;
    private String phone;
    private String photoUrl;
    private LocalDateTime joinedAt;
    private Double payRate;
    private double totalHours;
    private int missingLogs;
    private LocalDate firstMissingDate;
    /** totalHours × payRate, or 0 once the period is marked paid. */
    private double owed;
    private boolean paid;
    private boolean onSite;
    /** "HH:mm" start time of the job they're currently on, when onSite. */
    private String onSiteSince;
    private List<TimesheetDayResponse> days;
    private List<ShiftResponse> shifts;
    /** Short workspace-unique ID such as MK-0004; null for admins. */
    private String employeeCode;
}
