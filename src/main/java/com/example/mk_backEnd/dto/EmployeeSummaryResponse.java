package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeSummaryResponse {

    private String id;
    private String fullName;
    private String userType;
    private String phone;
    private String photoUrl;
}
