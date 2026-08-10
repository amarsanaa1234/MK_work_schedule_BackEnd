package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkspaceLookupResponse {

    private String organizationId;
    private String businessName;
    private String address;
}
