package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * One day within a {@link TimesheetSummaryResponse}'s range: whether the employee worked
 * ({@code WORKED}, hours logged), was rostered on a job that day but hasn't had hours
 * logged yet ({@code MISSING}), or wasn't on any job at all that day ({@code OFF}).
 */
@Getter
@AllArgsConstructor
public class TimesheetDayResponse {

    private LocalDate date;
    private String status;
    private Double hoursWorked;
}
