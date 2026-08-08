package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    private String phone;

    @NotBlank
    private String role; // "ADMIN" эсвэл "EMPLOYEE"

    private String addressLine;

    private double latitude;

    private double longitude;
}
