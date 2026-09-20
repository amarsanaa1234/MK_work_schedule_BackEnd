package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.WorkHourEntry;
import com.example.mk_backEnd.dto.CreateJobAdRequest;
import com.example.mk_backEnd.dto.EmployeeDetailResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.EmployeeHoursResponse;
import com.example.mk_backEnd.dto.TimesheetSummaryResponse;
import com.example.mk_backEnd.dto.WorkHourEntrySummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    JobAd createJobAd(String adminId, CreateJobAdRequest request);

    /** Updates an existing job post's fields, leader, and flat crew list. */
    JobAdSummaryResponse updateJobAd(String adminId, String jobAdId, CreateJobAdRequest request);

    /** Every crew member (leader included) on a job post, with hours logged so far, if any. */
    List<EmployeeHoursResponse> getJobHours(String adminId, String jobAdId);

    Assignment assignEmployee(String adminId, String jobAdId, String employeeId);

    Assignment setLead(String adminId, String jobAdId, String employeeId);

    WorkHourEntry recordWorkedHours(String adminId, String assignmentId, LocalDate date, double hours);

    /**
     * Records hours for an employee against a job post, creating the underlying
     * {@link Assignment} first if the employee was only ever added as flat crew
     * (via job creation) and never formally assigned.
     */
    WorkHourEntry recordHoursForJob(String adminId, String jobAdId, String employeeId, LocalDate date, double hours);

    List<WorkHourEntrySummaryResponse> recentWorkHours(String adminId, String employeeId);

    List<Assignment> viewSchedule(LocalDate from, LocalDate to);

    /** Job ads created within the calling admin's own workspace, for the admin dashboard. */
    List<JobAdSummaryResponse> listJobAds(String adminId, LocalDate from, LocalDate to);

    /** Employees in the calling admin's workspace, with pay rate, for the Pay rates screen. */
    List<EmployeeDetailResponse> listEmployeesWithRates(String adminId);

    void updatePayRate(String adminId, String employeeId, double payRate);

    /** Per-employee hours logged vs. missing over a date range, for the Timesheets screen. */
    List<TimesheetSummaryResponse> getTimesheets(String adminId, LocalDate from, LocalDate to);
}
