package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Address;
import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.JobAd;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeService {

    List<JobAd> viewMyJobAds(String employeeId);

    Address viewJobLocation(String jobAdId);

    double distanceFromHome(String employeeId, String jobAdId);

    List<Assignment> viewSchedule(String employeeId, LocalDate from, LocalDate to);
}
