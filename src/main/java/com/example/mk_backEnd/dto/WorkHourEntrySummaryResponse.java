package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class WorkHourEntrySummaryResponse {

    private LocalDate workDate;
    private double hoursWorked;
    private String jobAddressLine;
}
