package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One crew member's logged hours (if any) against a specific job post. */
@Getter
@AllArgsConstructor
public class EmployeeHoursResponse {

    private String employeeId;
    private String fullName;
    private String photoUrl;
    /** Null if nothing has been logged yet for this employee on this job. */
    private Double hoursWorked;
}
