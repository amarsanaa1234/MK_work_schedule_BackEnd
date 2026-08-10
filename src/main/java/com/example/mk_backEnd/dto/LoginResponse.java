package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private boolean success;
    private String userId;
    private String userType;
    private String fullName;
    private String token;
    private String organizationId;
}
