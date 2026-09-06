package com.example.mk_backEnd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Membership of one crew member (employee) on one job ad. Kept separate from
 * {@link Assignment} — that table carries the role-based admin assignment workflow,
 * this one is the flat crew list captured when a job is posted.
 */
@Entity
@Table(name = "job_ad_crew", uniqueConstraints = @UniqueConstraint(columnNames = {"job_ad_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
public class JobAdCrew {

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_ad_id", nullable = false)
    private JobAd jobAd;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        addedAt = LocalDateTime.now();
    }
}
