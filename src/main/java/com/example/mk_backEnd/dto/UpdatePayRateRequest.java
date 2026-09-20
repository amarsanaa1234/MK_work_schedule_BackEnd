package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePayRateRequest {

    @PositiveOrZero
    private double payRate;
}
