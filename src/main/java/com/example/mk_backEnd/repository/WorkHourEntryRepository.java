package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.WorkHourEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkHourEntryRepository extends JpaRepository<WorkHourEntry, String> {
}
