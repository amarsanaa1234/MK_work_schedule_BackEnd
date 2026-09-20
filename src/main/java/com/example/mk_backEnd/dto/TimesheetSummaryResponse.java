package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * One employee's timesheet summary over a date range: hours actually logged
 * ({@link com.example.mk_backEnd.domain.WorkHourEntry}) against every day they were
 * rostered on a job in the range, how many of those days have no hours logged yet, and
 * a day-by-day breakdown ({@link TimesheetDayResponse}) for rendering a calendar strip.
 */
@Getter
@AllArgsConstructor
public class TimesheetSummaryResponse {

    private String employeeId;
    private String employeeFullName;
    private String photoUrl;
    private double totalHours;
    private int loggedDays;
    private int missingLogs;
    private List<TimesheetDayResponse> days;
}
