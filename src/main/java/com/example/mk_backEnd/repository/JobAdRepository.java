package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.JobAd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JobAdRepository extends JpaRepository<JobAd, String> {

    List<JobAd> findByCreatedById(String adminId);

    List<JobAd> findByWorkDateBetween(LocalDate from, LocalDate to);
}
