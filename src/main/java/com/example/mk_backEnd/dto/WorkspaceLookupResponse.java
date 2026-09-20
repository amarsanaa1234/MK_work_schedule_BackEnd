package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkspaceLookupResponse {

    private String organizationId;
    private String businessName;
    private String address;
    /** True when the workspace is at its plan's people limit, so nobody new can join. */
    private boolean full;
    private int maxPeople;
}
