package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.JobAdCrew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobAdCrewRepository extends JpaRepository<JobAdCrew, String> {

    List<JobAdCrew> findByJobAdId(String jobAdId);

    List<JobAdCrew> findByEmployeeId(String employeeId);

    boolean existsByJobAdIdAndEmployeeId(String jobAdId, String employeeId);
}
