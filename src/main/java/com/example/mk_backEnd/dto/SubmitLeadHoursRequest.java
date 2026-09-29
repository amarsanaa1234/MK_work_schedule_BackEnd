package com.example.mk_backEnd.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** The lead's one-time submission of every crew member's hours for a job. */
@Getter
@Setter
public class SubmitLeadHoursRequest {

    @NotEmpty
    @Valid
    private List<Entry> entries;

    @Getter
    @Setter
    public static class Entry {

        @NotBlank
        private String employeeId;

        @PositiveOrZero
        private double hoursWorked;
    }
}
