package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** The logged-in user's own profile — works for both Admin and Employee. */
@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String userType;
    private String fullName;
    private String username;
    private String phone;
    private String photoUrl;
    private LocalDateTime createdAt;
    /** Only populated for Employees; null for Admins. */
    private Double payRate;
}
