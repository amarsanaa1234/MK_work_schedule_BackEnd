package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RecordJobHoursRequest {

    @NotBlank
    private String employeeId;

    @NotNull
    private LocalDate workDate;

    @PositiveOrZero
    private double hoursWorked;
}
