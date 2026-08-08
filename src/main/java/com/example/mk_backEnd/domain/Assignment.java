package com.example.mk_backEnd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Assignment {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private AssignmentRole role = AssignmentRole.WORKER;

    private LocalDate assignedAt;

    @ManyToOne
    @JoinColumn(name = "job_ad_id")
    private JobAd jobAd;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @JsonIgnore
    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private WorkHourEntry workHourEntry;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (assignedAt == null) {
            assignedAt = LocalDate.now();
        }
    }
}
