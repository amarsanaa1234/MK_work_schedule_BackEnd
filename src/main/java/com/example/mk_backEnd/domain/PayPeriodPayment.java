package com.example.mk_backEnd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Records that an employee's hours for one fixed pay period have been paid out. */
@Entity
@Table(name = "pay_period_payment", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "period_start"}))
@Getter
@Setter
@NoArgsConstructor
public class PayPeriodPayment {

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private double amount;

    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "paid_by")
    private Admin paidBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        paidAt = LocalDateTime.now();
    }
}
