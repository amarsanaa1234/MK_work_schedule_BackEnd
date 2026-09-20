package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Address;
import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.WorkHourEntry;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.WorkHourEntrySummaryResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AssignmentRepository;
import com.example.mk_backEnd.repository.EmployeeRepository;
import com.example.mk_backEnd.repository.JobAdCrewRepository;
import com.example.mk_backEnd.repository.JobAdRepository;
import com.example.mk_backEnd.repository.WorkHourEntryRepository;
import com.example.mk_backEnd.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final JobAdRepository jobAdRepository;
    private final AssignmentRepository assignmentRepository;
    private final JobAdCrewRepository jobAdCrewRepository;
    private final WorkHourEntryRepository workHourEntryRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                                JobAdRepository jobAdRepository,
                                AssignmentRepository assignmentRepository,
                                JobAdCrewRepository jobAdCrewRepository,
                                WorkHourEntryRepository workHourEntryRepository) {
        this.employeeRepository = employeeRepository;
        this.jobAdRepository = jobAdRepository;
        this.assignmentRepository = assignmentRepository;
        this.jobAdCrewRepository = jobAdCrewRepository;
        this.workHourEntryRepository = workHourEntryRepository;
    }

    @Override
    public List<JobAd> viewMyJobAds(String employeeId) {
        findEmployee(employeeId);
        // Jobs are actually assigned to an employee via the flat crew list (or the leader
        // field) at post/edit time — Assignment only exists once hours have been logged
        // against that employee, so it's the wrong source of truth for "what am I on".
        LinkedHashSet<JobAd> jobs = new LinkedHashSet<>(jobAdRepository.findByLeaderId(employeeId));
        jobAdCrewRepository.findByEmployeeId(employeeId).forEach(crew -> jobs.add(crew.getJobAd()));
        return List.copyOf(jobs);
    }

    @Override
    public List<JobAdSummaryResponse> viewMyJobAdsSummary(String employeeId, LocalDate from, LocalDate to) {
        return viewMyJobAds(employeeId).stream()
                .filter(jobAd -> !jobAd.getWorkDate().isBefore(from) && !jobAd.getWorkDate().isAfter(to))
                .sorted(Comparator.comparing(JobAd::getWorkDate)
                        .thenComparing(JobAd::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toSummary)
                .toList();
    }

    private JobAdSummaryResponse toSummary(JobAd jobAd) {
        EmployeeSummaryResponse leader = jobAd.getLeader() == null ? null : toEmployeeSummary(jobAd.getLeader());
        List<EmployeeSummaryResponse> crew = jobAdCrewRepository.findByJobAdId(jobAd.getId()).stream()
                .map(c -> toEmployeeSummary(c.getEmployee()))
                .toList();

        return new JobAdSummaryResponse(
                jobAd.getId(),
                jobAd.getTitle(),
                jobAd.getJobType(),
                jobAd.getTruck(),
                jobAd.getNotes(),
                jobAd.getStatus().name(),
                jobAd.getWorkDate(),
                jobAd.getStartTime(),
                jobAd.getLocation() == null ? null : jobAd.getLocation().getAddressLine(),
                jobAd.getRequiredCount(),
                leader,
                crew,
                jobAd.getCreatedAt());
    }

    private EmployeeSummaryResponse toEmployeeSummary(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(), employee.getFullName(), "Employee", employee.getPhone(), employee.getPhotoUrl());
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
    public List<WorkHourEntrySummaryResponse> getMyWorkHours(String employeeId, LocalDate from, LocalDate to) {
        findEmployee(employeeId);
        return workHourEntryRepository
                .findByAssignment_Employee_IdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, from, to)
                .stream()
                .map(entry -> new WorkHourEntrySummaryResponse(
                        entry.getWorkDate(),
                        entry.getHoursWorked(),
                        entry.getAssignment().getJobAd().getLocation() == null
                                ? null
                                : entry.getAssignment().getJobAd().getLocation().getAddressLine()))
                .toList();
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
