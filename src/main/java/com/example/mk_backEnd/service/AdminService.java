package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.WorkHourEntry;
import com.example.mk_backEnd.dto.CreateJobAdRequest;
import com.example.mk_backEnd.dto.EmployeeDetailResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.EmployeeHoursResponse;
import com.example.mk_backEnd.dto.EmployeeOverviewResponse;
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

    /**
     * Per-employee day strip (worked / missing / upcoming / off) and hours over a date range,
     * for the Timesheets screen.
     */
    List<TimesheetSummaryResponse> getTimesheets(String adminId, LocalDate from, LocalDate to);

    /** Everyone in the admin's workspace with hours, pay and paid status for one pay period. */
    List<EmployeeOverviewResponse> getEmployeeOverview(String adminId, LocalDate from, LocalDate to);

    /**
     * The same hours/pay/day breakdown for one employee looking at themselves (their own
     * profile). Lives here because it shares all its logic with {@link #getEmployeeOverview}.
     */
    EmployeeOverviewResponse getMyOverview(String employeeId, LocalDate from, LocalDate to);

    /** Marks the pay period starting on {@code from} as paid for an employee. Idempotent. */
    void markPeriodPaid(String adminId, String employeeId, LocalDate from, LocalDate to);

    void unmarkPeriodPaid(String adminId, String employeeId, LocalDate from);

    /**
     * Removes an employee from the workspace: they lose access and come off upcoming jobs,
     * while their past hours and pay history are kept.
     */
    void removeEmployee(String adminId, String employeeId);
}
