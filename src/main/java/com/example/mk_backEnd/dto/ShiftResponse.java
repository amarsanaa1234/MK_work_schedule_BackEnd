package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * One job an employee was on within a period. {@code status} is WORKED (hours
 * logged), MISSING (a past job with no hours logged) or UPCOMING (today or later).
 */
@Getter
@AllArgsConstructor
public class ShiftResponse {

    private LocalDate date;
    private String jobAdId;
    private String addressLine;
    private Double hoursWorked;
    private String status;
    private String jobType;
    /** True when this person was the lead on the job. */
    private boolean lead;
}
