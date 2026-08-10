package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkspaceRequest {

    @NotBlank
    private String businessName;

    private String abn;

    private String industry;

    private String address;

    @NotBlank
    private String adminName;

    @NotBlank
    private String adminEmail;

    @NotBlank
    private String adminPassword;
}
