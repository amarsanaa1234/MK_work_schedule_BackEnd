package com.example.mk_backEnd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** An in-app notification for one crew member, e.g. a job they're on was posted or edited. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee recipient;

    @Column(nullable = false, length = 500)
    private String message;

    @ManyToOne
    @JoinColumn(name = "job_ad_id")
    private JobAd jobAd;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // "read" is a reserved word in MySQL — must be mapped to a non-reserved column name.
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

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
