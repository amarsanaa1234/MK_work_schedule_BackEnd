package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

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

    private String abn;

    @NotBlank
    private String organizationId;

    private String addressLine;

    private double latitude;

    private double longitude;

    private MultipartFile photo;
}
