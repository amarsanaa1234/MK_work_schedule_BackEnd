package com.example.mk_backEnd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class Workspace {

    @Id
    private String id;

    @Column(name = "organization_id", unique = true, nullable = false, length = 8)
    private String organizationId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    private String abn;

    private String industry;

    private String address;

    private String phone;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
