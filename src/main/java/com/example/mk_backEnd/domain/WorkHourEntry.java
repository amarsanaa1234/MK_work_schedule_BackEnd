package com.example.mk_backEnd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkHourEntry {

    @Id
    private String id;

    private LocalDate workDate;

    private double hoursWorked;

    private LocalDateTime recordedAt;

    @OneToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin recordedBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
