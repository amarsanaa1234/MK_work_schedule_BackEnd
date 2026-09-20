package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Admin-only view of an employee, used by the Pay rates screen. */
@Getter
@AllArgsConstructor
public class EmployeeDetailResponse {

    private String id;
    private String fullName;
    private String phone;
    private String photoUrl;
    private Double payRate;
}
