package com.example.mk_backEnd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** The business details an existing Business admin gives to add one more workspace. */
@Getter
@Setter
public class AddWorkspaceRequest {

    @NotBlank
    private String businessName;

    private String abn;

    private String industry;

    private String address;
}
