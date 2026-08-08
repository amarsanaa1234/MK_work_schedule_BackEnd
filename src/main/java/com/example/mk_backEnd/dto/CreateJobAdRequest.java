package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateJobAdRequest {

    @NotBlank
    private String title;

    private String description;

    @Positive
    private int requiredCount;

    @NotNull
    private LocalDate workDate;

    @NotBlank
    private String addressLine;

    private double latitude;

    private double longitude;
}
