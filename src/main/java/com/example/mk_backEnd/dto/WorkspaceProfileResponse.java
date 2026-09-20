package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkspaceProfileResponse {

    private String organizationId;
    private String businessName;
    private String abn;
    private String industry;
    private String address;
    private String phone;
    private long crewCount;
    private long adminCount;
}
