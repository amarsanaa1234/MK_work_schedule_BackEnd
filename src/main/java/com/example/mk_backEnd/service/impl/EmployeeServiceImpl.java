package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Address;
import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.AssignmentRole;
import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.JobAdCrew;
import com.example.mk_backEnd.domain.WorkHourEntry;
import com.example.mk_backEnd.dto.EmployeeHoursResponse;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.SubmitLeadHoursRequest;
import com.example.mk_backEnd.dto.WorkHourEntrySummaryResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AssignmentRepository;
import com.example.mk_backEnd.repository.EmployeeRepository;
import com.example.mk_backEnd.repository.JobAdCrewRepository;
import com.example.mk_backEnd.repository.JobAdRepository;
import com.example.mk_backEnd.repository.WorkHourEntryRepository;
import com.example.mk_backEnd.service.EmployeeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
                jobAd.getInductionUrl(),
                jobAd.getNotes(),
                jobAd.getStatus().name(),
                jobAd.getWorkDate(),
                jobAd.getStartTime(),
                jobAd.getLocation() == null ? null : jobAd.getLocation().getAddressLine(),
                jobAd.getRequiredCount(),
                leader,
                crew,
                jobAd.getCreatedAt(),
                hoursLogged(jobAd));
    }

    private EmployeeSummaryResponse toEmployeeSummary(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(), employee.getFullName(), "Employee", employee.getPhone(), employee.getPhotoUrl());
    }

    private boolean hoursLogged(JobAd jobAd) {
        return jobAd.getLeadHoursSubmittedAt() != null
                || assignmentRepository.findByJobAdId(jobAd.getId()).stream().anyMatch(a -> a.getWorkHourEntry() != null);
    }

    /** Leader first, then the rest of the crew — the same order the admin's hours screen uses. */
    private List<Employee> jobPeople(JobAd jobAd) {
        List<Employee> people = new ArrayList<>();
        if (jobAd.getLeader() != null) {
            people.add(jobAd.getLeader());
        }
        for (JobAdCrew crew : jobAdCrewRepository.findByJobAdId(jobAd.getId())) {
            if (jobAd.getLeader() == null || !crew.getEmployee().getId().equals(jobAd.getLeader().getId())) {
                people.add(crew.getEmployee());
            }
        }
        return people;
    }

    private JobAd findJobLedBy(String employeeId, String jobAdId) {
        findEmployee(employeeId);
        JobAd jobAd = findJobAd(jobAdId);
        if (jobAd.getLeader() == null || !jobAd.getLeader().getId().equals(employeeId)) {
            throw new AccessDeniedException("Зөвхөн энэ ажлын ахлагч цаг оруулах боломжтой.");
        }
        return jobAd;
    }

    @Override
    public List<EmployeeHoursResponse> getJobHoursAsLead(String employeeId, String jobAdId) {
        JobAd jobAd = findJobLedBy(employeeId, jobAdId);
        List<Assignment> assignments = assignmentRepository.findByJobAdId(jobAdId);
        return jobPeople(jobAd).stream()
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
    public void submitHoursAsLead(String employeeId, String jobAdId, List<SubmitLeadHoursRequest.Entry> entries) {
        JobAd jobAd = findJobLedBy(employeeId, jobAdId);
        if (jobAd.getWorkDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Ажлын өдөр болоогүй байхад цаг оруулах боломжгүй.");
        }
        if (hoursLogged(jobAd)) {
            throw new BadRequestException("Энэ ажлын цаг аль хэдийн бүртгэгдсэн байна. Засах шаардлагатай бол админд хандана уу.");
        }

        Map<String, Employee> people = new LinkedHashMap<>();
        jobPeople(jobAd).forEach(e -> people.put(e.getId(), e));
        Map<String, Double> hoursById = new HashMap<>();
        for (SubmitLeadHoursRequest.Entry entry : entries) {
            if (!people.containsKey(entry.getEmployeeId())) {
                throw new BadRequestException("Энэ ажилтан уг ажилд ороогүй байна: " + entry.getEmployeeId());
            }
            hoursById.put(entry.getEmployeeId(), entry.getHoursWorked());
        }
        if (!hoursById.keySet().equals(people.keySet())) {
            throw new BadRequestException("Багийн бүх гишүүний цагийг оруулна уу.");
        }

        List<Assignment> assignments = assignmentRepository.findByJobAdId(jobAdId);
        for (Employee employee : people.values()) {
            Assignment assignment = assignments.stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        Assignment created = new Assignment();
                        created.setJobAd(jobAd);
                        created.setEmployee(employee);
                        created.setRole(employee.getId().equals(employeeId) ? AssignmentRole.LEAD : AssignmentRole.WORKER);
                        return assignmentRepository.save(created);
                    });

            WorkHourEntry entry = new WorkHourEntry();
            entry.setAssignment(assignment);
            entry.setWorkDate(jobAd.getWorkDate());
            entry.setHoursWorked(hoursById.get(employee.getId()));
            workHourEntryRepository.save(entry);
        }

        jobAd.setLeadHoursSubmittedAt(LocalDateTime.now());
        jobAdRepository.save(jobAd);
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
        return employeeRepository.findByWorkspaceIdAndRemovedAtIsNull(workspaceId).stream()
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
