package com.example.mk_backEnd.domain;

public enum AssignmentRole {

    LEAD("АХЛАГЧ"),
    WORKER("ЭНГИЙН_АЖИЛТАН");

    private final String label;

    AssignmentRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
