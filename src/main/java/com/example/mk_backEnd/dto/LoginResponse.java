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
    private String photoUrl;
    private String industry;
    private String address;
    /** Name of the workspace the user is currently in (the active one, for admins with several). */
    private String businessName;
    /** How many workspaces this user can open - more than 1 only for Business owners. */
    private int workspaceCount;
}
