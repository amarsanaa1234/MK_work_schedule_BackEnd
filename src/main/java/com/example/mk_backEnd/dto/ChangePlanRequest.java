package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePlanRequest {

    @NotBlank
    private String plan;

    /** MONTHLY (default) or YEARLY. */
    private String interval;
}
