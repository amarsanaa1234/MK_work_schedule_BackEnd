package com.example.mk_backEnd.domain;

public enum JobStatus {

    DRAFT("НООРОГ"),
    OPEN("НЭЭЛТТЭЙ"),
    CLOSED("ДУУССАН"),
    CANCELLED("ЦУЦЛАГДСАН");

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
