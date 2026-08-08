package com.example.mk_backEnd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog {

    @Id
    private String id;

    private String action;

    private LocalDateTime timestamp;

    private String ipAddress;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
