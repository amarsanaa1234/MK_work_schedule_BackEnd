package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.*;
import com.example.mk_backEnd.dto.CreateJobAdRequest;
import com.example.mk_backEnd.dto.EmployeeDetailResponse;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.EmployeeHoursResponse;
import com.example.mk_backEnd.dto.TimesheetDayResponse;
import com.example.mk_backEnd.dto.TimesheetSummaryResponse;
import com.example.mk_backEnd.dto.WorkHourEntrySummaryResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.*;
import com.example.mk_backEnd.service.ActivityLogService;
import com.example.mk_backEnd.service.AdminService;
import com.example.mk_backEnd.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final EmployeeRepository employeeRepository;
    private final JobAdRepository jobAdRepository;
    private final JobAdCrewRepository jobAdCrewRepository;
    private final AssignmentRepository assignmentRepository;
    private final WorkHourEntryRepository workHourEntryRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public AdminServiceImpl(AdminRepository adminRepository,
                             EmployeeRepository employeeRepository,
                             JobAdRepository jobAdRepository,
                             JobAdCrewRepository jobAdCrewRepository,
                             AssignmentRepository assignmentRepository,
                             WorkHourEntryRepository workHourEntryRepository,
                             ActivityLogService activityLogService,
                             NotificationService notificationService) {
        this.adminRepository = adminRepository;
        this.employeeRepository = employeeRepository;
        this.jobAdRepository = jobAdRepository;
        this.jobAdCrewRepository = jobAdCrewRepository;
        this.assignmentRepository = assignmentRepository;
        this.workHourEntryRepository = workHourEntryRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public JobAd createJobAd(String adminId, CreateJobAdRequest request) {
        Admin admin = findAdmin(adminId);

        List<String> crewIds = request.getCrewIds() == null ? List.of() : request.getCrewIds();

        Address location = new Address();
        location.setAddressLine(request.getAddressLine());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());

        JobAd jobAd = new JobAd();
        String title = request.getTitle();
        jobAd.setTitle((title == null || title.isBlank())
                ? (request.getJobType() == null ? "Job" : request.getJobType()) + " — " + request.getAddressLine()
                : title);
        jobAd.setDescription(request.getDescription());
        jobAd.setRequiredCount(Math.max(request.getRequiredCount(), crewIds.size()));
        jobAd.setWorkDate(request.getWorkDate());
        jobAd.setStartTime(request.getStartTime());
        jobAd.setJobType(request.getJobType());
        jobAd.setTruck(request.getTruck());
        jobAd.setNotes(request.getNotes());
        jobAd.setStatus(request.isDraft() ? JobStatus.DRAFT : JobStatus.OPEN);
        jobAd.setLocation(location);
        jobAd.setCreatedBy(admin);

        if (request.getLeaderId() != null && !request.getLeaderId().isBlank()) {
            jobAd.setLeader(findEmployee(request.getLeaderId()));
        }

        JobAd saved = jobAdRepository.save(jobAd);

        for (String employeeId : crewIds) {
            JobAdCrew crew = new JobAdCrew();
            crew.setJobAd(saved);
            crew.setEmployee(findEmployee(employeeId));
            jobAdCrewRepository.save(crew);
        }

        activityLogService.log(adminId, "CREATE_JOB_AD:" + saved.getId(), null);
        if (!request.isDraft()) {
            notificationService.notifyCrew(saved, "New job posted: " + saved.getTitle());
        }
        return saved;
    }

    @Override
    @Transactional
    public JobAdSummaryResponse updateJobAd(String adminId, String jobAdId, CreateJobAdRequest request) {
        Admin admin = findAdmin(adminId);
        JobAd jobAd = findJobAd(jobAdId);
        if (!jobAd.getCreatedBy().getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new BadRequestException("Энэ ажлын зар танай байгууллагад харьяалагдахгүй байна.");
        }

        List<String> crewIds = request.getCrewIds() == null ? List.of() : request.getCrewIds();

        Address location = jobAd.getLocation() == null ? new Address() : jobAd.getLocation();
        location.setAddressLine(request.getAddressLine());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());

        String title = request.getTitle();
        jobAd.setTitle((title == null || title.isBlank())
                ? (request.getJobType() == null ? "Job" : request.getJobType()) + " — " + request.getAddressLine()
                : title);
        jobAd.setDescription(request.getDescription());
        jobAd.setRequiredCount(Math.max(request.getRequiredCount(), crewIds.size()));
        jobAd.setWorkDate(request.getWorkDate());
        jobAd.setStartTime(request.getStartTime());
        jobAd.setJobType(request.getJobType());
        jobAd.setTruck(request.getTruck());
        jobAd.setNotes(request.getNotes());
        jobAd.setStatus(request.isDraft() ? JobStatus.DRAFT : JobStatus.OPEN);
        jobAd.setLocation(location);
        jobAd.setLeader(request.getLeaderId() == null || request.getLeaderId().isBlank()
                ? null
                : findEmployee(request.getLeaderId()));

        JobAd saved = jobAdRepository.save(jobAd);

        // deleteByJobAdId only marks the rows for removal in the persistence context —
        // without an explicit flush, the DELETE can reach the DB after the INSERTs below
        // for employees who are still on the crew, tripping the (job_ad_id, employee_id)
        // unique constraint.
        jobAdCrewRepository.deleteByJobAdId(jobAdId);
        jobAdCrewRepository.flush();
        for (String employeeId : crewIds) {
            JobAdCrew crew = new JobAdCrew();
            crew.setJobAd(saved);
            crew.setEmployee(findEmployee(employeeId));
            jobAdCrewRepository.save(crew);
        }

        activityLogService.log(adminId, "UPDATE_JOB_AD:" + saved.getId(), null);
        if (!request.isDraft()) {
            notificationService.notifyCrew(saved, "Job updated: " + saved.getTitle());
        }
        return toSummary(saved);
    }

    @Override
    public List<EmployeeHoursResponse> getJobHours(String adminId, String jobAdId) {
        Admin admin = findAdmin(adminId);
        JobAd jobAd = findJobAd(jobAdId);
        if (!jobAd.getCreatedBy().getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new BadRequestException("Энэ ажлын зар танай байгууллагад харьяалагдахгүй байна.");
        }

        List<Employee> people = new java.util.ArrayList<>();
        if (jobAd.getLeader() != null) {
            people.add(jobAd.getLeader());
        }
        for (JobAdCrew crew : jobAdCrewRepository.findByJobAdId(jobAdId)) {
            if (jobAd.getLeader() == null || !crew.getEmployee().getId().equals(jobAd.getLeader().getId())) {
                people.add(crew.getEmployee());
            }
        }

        List<Assignment> assignments = assignmentRepository.findByJobAdId(jobAdId);

        return people.stream()
                .map(employee -> {
                    Double hours = assignments.stream()
                            .filter(a -> a.getEmployee().getId().equals(employee.getId()))
                            .findFirst()
                            .map(Assignment::getWorkHourEntry)
                            .map(WorkHourEntry::getHoursWorked)
                            .orElse(null);
                    return new EmployeeHoursResponse(employee.getId(), employee.getFullName(), employee.getPhotoUrl(), hours);
                })
                .toList();
    }

    @Override
    @Transactional
    public Assignment assignEmployee(String adminId, String jobAdId, String employeeId) {
        findAdmin(adminId);
        JobAd jobAd = findJobAd(jobAdId);
        Employee employee = findEmployee(employeeId);

        if (assignmentRepository.existsByJobAdIdAndEmployeeId(jobAdId, employeeId)) {
            throw new BadRequestException("Энэ ажилтан уг ажлын зард аль хэдийн томилогдсон байна.");
        }
        if (jobAd.getAssignments().size() >= jobAd.getRequiredCount()) {
            throw new BadRequestException("Ажлын зарын шаардлагатай орон тоо дүүрсэн байна.");
        }

        Assignment assignment = new Assignment();
        assignment.setJobAd(jobAd);
        assignment.setEmployee(employee);
        assignment.setRole(AssignmentRole.WORKER);

        Assignment saved = assignmentRepository.save(assignment);
        activityLogService.log(adminId, "ASSIGN_EMPLOYEE:" + employeeId + "->" + jobAdId, null);
        return saved;
    }

    @Override
    @Transactional
    public Assignment setLead(String adminId, String jobAdId, String employeeId) {
        findAdmin(adminId);
        JobAd jobAd = findJobAd(jobAdId);
        Employee employee = findEmployee(employeeId);

        Assignment assignment = jobAd.getAssignments().stream()
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Ажилтан энэ ажлын зард томилогдоогүй байна."));

        assignment.setRole(AssignmentRole.LEAD);
        Assignment saved = assignmentRepository.save(assignment);
        activityLogService.log(adminId, "SET_LEAD:" + employeeId + "@" + jobAdId, null);
        return saved;
    }

    @Override
    @Transactional
    public WorkHourEntry recordWorkedHours(String adminId, String assignmentId, LocalDate date, double hours) {
        Admin admin = findAdmin(adminId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Томилолт олдсонгүй: " + assignmentId));

        WorkHourEntry entry = assignment.getWorkHourEntry();
        if (entry == null) {
            entry = new WorkHourEntry();
            entry.setAssignment(assignment);
        }
        entry.setWorkDate(date);
        entry.setHoursWorked(hours);
        entry.setRecordedBy(admin);

        WorkHourEntry saved = workHourEntryRepository.save(entry);
        activityLogService.log(adminId, "RECORD_HOURS:" + assignmentId, null);
        return saved;
    }

    @Override
    @Transactional
    public WorkHourEntry recordHoursForJob(String adminId, String jobAdId, String employeeId, LocalDate date, double hours) {
        Admin admin = findAdmin(adminId);
        JobAd jobAd = findJobAd(jobAdId);
        Employee employee = findEmployee(employeeId);

        Assignment assignment = assignmentRepository.findByJobAdId(jobAdId).stream()
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .findFirst()
                .orElseGet(() -> {
                    Assignment created = new Assignment();
                    created.setJobAd(jobAd);
                    created.setEmployee(employee);
                    created.setRole(AssignmentRole.WORKER);
                    return assignmentRepository.save(created);
                });

        WorkHourEntry entry = assignment.getWorkHourEntry();
        if (entry == null) {
            entry = new WorkHourEntry();
            entry.setAssignment(assignment);
        }
        entry.setWorkDate(date);
        entry.setHoursWorked(hours);
        entry.setRecordedBy(admin);

        WorkHourEntry saved = workHourEntryRepository.save(entry);
        activityLogService.log(adminId, "RECORD_HOURS:" + assignment.getId(), null);
        return saved;
    }

    @Override
    public List<WorkHourEntrySummaryResponse> recentWorkHours(String adminId, String employeeId) {
        findAdmin(adminId);
        return workHourEntryRepository.findTop5ByAssignment_Employee_IdOrderByWorkDateDesc(employeeId).stream()
                .map(entry -> new WorkHourEntrySummaryResponse(
                        entry.getWorkDate(),
                        entry.getHoursWorked(),
                        entry.getAssignment().getJobAd().getLocation() == null
                                ? null
                                : entry.getAssignment().getJobAd().getLocation().getAddressLine()))
                .toList();
    }

    @Override
    public List<Assignment> viewSchedule(LocalDate from, LocalDate to) {
        return assignmentRepository.findByJobAd_WorkDateBetween(from, to);
    }

    @Override
    public List<JobAdSummaryResponse> listJobAds(String adminId, LocalDate from, LocalDate to) {
        Admin admin = findAdmin(adminId);
        String workspaceId = admin.getWorkspace().getId();
        return jobAdRepository.findByCreatedBy_Workspace_IdAndWorkDateBetween(workspaceId, from, to).stream()
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
    public List<EmployeeDetailResponse> listEmployeesWithRates(String adminId) {
        Admin admin = findAdmin(adminId);
        return employeeRepository.findByWorkspaceId(admin.getWorkspace().getId()).stream()
                .map(e -> new EmployeeDetailResponse(
                        e.getId(), e.getFullName(), e.getPhone(), e.getPhotoUrl(), e.getPayRate()))
                .toList();
    }

    @Override
    @Transactional
    public void updatePayRate(String adminId, String employeeId, double payRate) {
        Admin admin = findAdmin(adminId);
        Employee employee = findEmployee(employeeId);
        if (!employee.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new BadRequestException("Энэ ажилтан таны байгууллагад харьяалагдахгүй байна.");
        }
        employee.setPayRate(payRate);
        employeeRepository.save(employee);
        activityLogService.log(adminId, "UPDATE_PAY_RATE:" + employeeId, null);
    }

    @Override
    public List<TimesheetSummaryResponse> getTimesheets(String adminId, LocalDate from, LocalDate to) {
        Admin admin = findAdmin(adminId);
        List<Employee> employees = employeeRepository.findByWorkspaceId(admin.getWorkspace().getId());

        return employees.stream()
                .map(employee -> buildTimesheetSummary(employee, from, to))
                .toList();
    }

    private TimesheetSummaryResponse buildTimesheetSummary(Employee employee, LocalDate from, LocalDate to) {
        // Jobs are assigned via the flat crew list (or the leader field) at post/edit time —
        // same source of truth an employee's own feed uses. Assignment rows only exist once
        // hours have been logged, so they can't tell us which days someone was actually
        // rostered on vs. genuinely had the day off.
        java.util.Set<LocalDate> assignedDates = new java.util.HashSet<>();
        jobAdRepository.findByLeaderId(employee.getId()).forEach(jobAd -> assignedDates.add(jobAd.getWorkDate()));
        jobAdCrewRepository.findByEmployeeId(employee.getId())
                .forEach(crew -> assignedDates.add(crew.getJobAd().getWorkDate()));

        java.util.Map<LocalDate, Double> hoursByDate = new java.util.HashMap<>();
        workHourEntryRepository
                .findByAssignment_Employee_IdAndWorkDateBetweenOrderByWorkDateDesc(employee.getId(), from, to)
                .forEach(entry -> hoursByDate.put(entry.getWorkDate(), entry.getHoursWorked()));

        List<TimesheetDayResponse> days = new java.util.ArrayList<>();
        double totalHours = 0;
        int loggedDays = 0;
        int missingLogs = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            Double hours = hoursByDate.get(date);
            String status;
            if (hours != null) {
                status = "WORKED";
                totalHours += hours;
                loggedDays++;
            } else if (assignedDates.contains(date)) {
                status = "MISSING";
                missingLogs++;
            } else {
                status = "OFF";
            }
            days.add(new TimesheetDayResponse(date, status, hours));
        }

        return new TimesheetSummaryResponse(
                employee.getId(), employee.getFullName(), employee.getPhotoUrl(),
                totalHours, loggedDays, missingLogs, days);
    }

    private Admin findAdmin(String adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Админ олдсонгүй: " + adminId));
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
