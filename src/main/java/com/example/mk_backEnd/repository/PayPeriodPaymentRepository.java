package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.PayPeriodPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PayPeriodPaymentRepository extends JpaRepository<PayPeriodPayment, String> {

    Optional<PayPeriodPayment> findByEmployeeIdAndPeriodStart(String employeeId, LocalDate periodStart);

    boolean existsByEmployeeIdAndPeriodStart(String employeeId, LocalDate periodStart);
}
