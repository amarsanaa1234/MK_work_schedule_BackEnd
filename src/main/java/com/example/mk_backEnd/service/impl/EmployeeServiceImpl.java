package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Address;
import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AssignmentRepository;
import com.example.mk_backEnd.repository.EmployeeRepository;
import com.example.mk_backEnd.repository.JobAdRepository;
import com.example.mk_backEnd.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final JobAdRepository jobAdRepository;
    private final AssignmentRepository assignmentRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                                JobAdRepository jobAdRepository,
                                AssignmentRepository assignmentRepository) {
        this.employeeRepository = employeeRepository;
        this.jobAdRepository = jobAdRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public List<JobAd> viewMyJobAds(String employeeId) {
        findEmployee(employeeId);
        return assignmentRepository.findByEmployeeId(employeeId).stream()
                .map(Assignment::getJobAd)
                .distinct()
                .toList();
    }

    @Override
    public Address viewJobLocation(String jobAdId) {
        return findJobAd(jobAdId).getLocation();
    }

    @Override
    public double distanceFromHome(String employeeId, String jobAdId) {
        Employee employee = findEmployee(employeeId);
        JobAd jobAd = findJobAd(jobAdId);

        if (employee.getAddress() == null) {
            throw new BadRequestException("Ажилтны гэрийн хаяг бүртгэгдээгүй байна.");
        }
        if (jobAd.getLocation() == null) {
            throw new BadRequestException("Ажлын зарын байршил бүртгэгдээгүй байна.");
        }
        return employee.getAddress().distanceTo(jobAd.getLocation());
    }

    @Override
    public List<Assignment> viewSchedule(String employeeId, LocalDate from, LocalDate to) {
        findEmployee(employeeId);
        return assignmentRepository.findByEmployeeIdAndJobAd_WorkDateBetween(employeeId, from, to);
    }

    @Override
    public List<EmployeeSummaryResponse> getAllEmployees(String workspaceId) {
        return employeeRepository.findByWorkspaceId(workspaceId).stream()
                .map(e -> new EmployeeSummaryResponse(
                        e.getId(), e.getFullName(), "Employee", e.getPhone(), e.getPhotoUrl()))
                .toList();
    }

    private Employee findEmployee(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ажилтан олдсонгүй: " + employeeId));
    }

    private JobAd findJobAd(String jobAdId) {
        return jobAdRepository.findById(jobAdId)
                .orElseThrow(() -> new ResourceNotFoundException("Ажлын зар олдсонгүй: " + jobAdId));
    }
}
