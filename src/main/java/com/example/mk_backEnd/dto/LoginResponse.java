package com.example.mk_backEnd.dto;

import com.example.mk_backEnd.domain.Address;
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
    private String photoUrl;
    private String industry;
    private String address;
}
