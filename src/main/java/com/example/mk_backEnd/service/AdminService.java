package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.WorkHourEntry;
import com.example.mk_backEnd.dto.CreateJobAdRequest;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    JobAd createJobAd(String adminId, CreateJobAdRequest request);

    Assignment assignEmployee(String adminId, String jobAdId, String employeeId);

    Assignment setLead(String adminId, String jobAdId, String employeeId);

    WorkHourEntry recordWorkedHours(String adminId, String assignmentId, LocalDate date, double hours);

    List<Assignment> viewSchedule(LocalDate from, LocalDate to);
}
