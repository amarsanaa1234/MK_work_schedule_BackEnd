package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RecordWorkedHoursRequest {

    @NotBlank
    private String assignmentId;

    @NotNull
    private LocalDate workDate;

    @PositiveOrZero
    private double hoursWorked;
}
