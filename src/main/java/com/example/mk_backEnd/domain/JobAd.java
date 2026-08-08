package com.example.mk_backEnd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin createdBy;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address location;

    @JsonIgnore
    @OneToMany(mappedBy = "jobAd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
