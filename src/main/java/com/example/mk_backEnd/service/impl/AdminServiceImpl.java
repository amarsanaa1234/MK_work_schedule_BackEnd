package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.*;
import com.example.mk_backEnd.dto.CreateJobAdRequest;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.*;
import com.example.mk_backEnd.service.ActivityLogService;
import com.example.mk_backEnd.service.AdminService;
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

    public AdminServiceImpl(AdminRepository adminRepository,
                             EmployeeRepository employeeRepository,
                             JobAdRepository jobAdRepository,
                             JobAdCrewRepository jobAdCrewRepository,
                             AssignmentRepository assignmentRepository,
                             WorkHourEntryRepository workHourEntryRepository,
                             ActivityLogService activityLogService) {
        this.adminRepository = adminRepository;
        this.employeeRepository = employeeRepository;
        this.jobAdRepository = jobAdRepository;
        this.jobAdCrewRepository = jobAdCrewRepository;
        this.assignmentRepository = assignmentRepository;
        this.workHourEntryRepository = workHourEntryRepository;
        this.activityLogService = activityLogService;
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
        return saved;
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
    public List<Assignment> viewSchedule(LocalDate from, LocalDate to) {
        return assignmentRepository.findByJobAd_WorkDateBetween(from, to);
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
