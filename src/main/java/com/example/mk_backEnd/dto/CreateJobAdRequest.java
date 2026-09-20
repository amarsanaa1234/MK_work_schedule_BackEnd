package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class CreateJobAdRequest {

    private String title;

    private String description;

    private int requiredCount;

    @NotNull
    private LocalDate workDate;

    private LocalTime startTime;

    @NotBlank
    private String addressLine;

    private double latitude;

    private double longitude;

    private String jobType;

    private String inductionUrl;

    private String notes;

    private String leaderId;

    private List<String> crewIds;

    /** true = "Save as draft", false/omitted = publish straight away. */
    private boolean draft;
}
