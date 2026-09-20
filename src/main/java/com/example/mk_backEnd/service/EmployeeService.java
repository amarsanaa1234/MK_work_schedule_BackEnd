package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Address;
import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.WorkHourEntrySummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeService {

    /** Jobs the employee is on — as leader or flat crew, i.e. however the job was actually posted/edited. */
    List<JobAd> viewMyJobAds(String employeeId);

    /** Same job ads as {@link #viewMyJobAds}, flattened with crew names, filtered to a date range for the Home feed. */
    List<JobAdSummaryResponse> viewMyJobAdsSummary(String employeeId, LocalDate from, LocalDate to);

    Address viewJobLocation(String jobAdId);

    double distanceFromHome(String employeeId, String jobAdId);

    List<Assignment> viewSchedule(String employeeId, LocalDate from, LocalDate to);

    List<EmployeeSummaryResponse> getAllEmployees(String workspaceId);

    /** The employee's own logged hours over a date range, for their "My timesheet" screen. */
    List<WorkHourEntrySummaryResponse> getMyWorkHours(String employeeId, LocalDate from, LocalDate to);
}
