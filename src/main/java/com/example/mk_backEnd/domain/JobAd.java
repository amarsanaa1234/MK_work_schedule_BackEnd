package com.example.mk_backEnd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class JobAd {

    @Id
    private String id;

    private String title;

    private String description;

    private int requiredCount;

    private LocalDate workDate;

    private LocalTime startTime;

    @Column(name = "job_type")
    private String jobType;

    private String truck;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin createdBy;

    @ManyToOne
    @JoinColumn(name = "leader_id")
    private Employee leader;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address location;

    @JsonIgnore
    @OneToMany(mappedBy = "jobAd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "jobAd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobAdCrew> crew = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
