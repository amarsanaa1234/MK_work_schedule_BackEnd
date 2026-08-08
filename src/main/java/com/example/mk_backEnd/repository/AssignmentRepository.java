package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, String> {

    List<Assignment> findByEmployeeId(String employeeId);

    List<Assignment> findByJobAdId(String jobAdId);

    List<Assignment> findByJobAd_WorkDateBetween(LocalDate from, LocalDate to);

    List<Assignment> findByEmployeeIdAndJobAd_WorkDateBetween(String employeeId, LocalDate from, LocalDate to);

    boolean existsByJobAdIdAndEmployeeId(String jobAdId, String employeeId);
}
