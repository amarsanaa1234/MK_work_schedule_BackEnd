package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.WorkHourEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WorkHourEntryRepository extends JpaRepository<WorkHourEntry, String> {

    List<WorkHourEntry> findTop5ByAssignment_Employee_IdOrderByWorkDateDesc(String employeeId);

    List<WorkHourEntry> findByAssignment_Employee_IdAndWorkDateBetweenOrderByWorkDateDesc(
            String employeeId, LocalDate from, LocalDate to);
}
