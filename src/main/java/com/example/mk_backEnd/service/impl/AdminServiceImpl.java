package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.*;
import com.example.mk_backEnd.dto.CreateJobAdRequest;
import com.example.mk_backEnd.dto.EmployeeDetailResponse;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.dto.JobAdSummaryResponse;
import com.example.mk_backEnd.dto.EmployeeHoursResponse;
import com.example.mk_backEnd.dto.EmployeeOverviewResponse;
import com.example.mk_backEnd.dto.ShiftResponse;
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
    private final PayPeriodPaymentRepository payPeriodPaymentRepository;
    private final UserRepository userRepository;

    public AdminServiceImpl(AdminRepository adminRepository,
                             EmployeeRepository employeeRepository,
                             JobAdRepository jobAdRepository,
                             JobAdCrewRepository jobAdCrewRepository,
                             AssignmentRepository assignmentRepository,
                             WorkHourEntryRepository workHourEntryRepository,
                             ActivityLogService activityLogService,
                             NotificationService notificationService,
                             PayPeriodPaymentRepository payPeriodPaymentRepository,
                             UserRepository userRepository) {
        this.adminRepository = adminRepository;
        this.employeeRepository = employeeRepository;
        this.jobAdRepository = jobAdRepository;
        this.jobAdCrewRepository = jobAdCrewRepository;
        this.assignmentRepository = assignmentRepository;
        this.workHourEntryRepository = workHourEntryRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.payPeriodPaymentRepository = payPeriodPaymentRepository;
        this.userRepository = userRepository;
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
        jobAd.setInductionUrl(request.getInductionUrl());
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
        jobAd.setInductionUrl(request.getInductionUrl());
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
                jobAd.getInductionUrl(),
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
        return employeeRepository.findByWorkspaceIdAndRemovedAtIsNull(admin.getWorkspace().getId()).stream()
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

    /** One job an employee was on, with the hours logged against it (null if none yet). */
    private record Shift(LocalDate date, JobAd jobAd, Double hours) {}

    /**
     * Everything an employee did or is rostered for within [from, to]. Jobs are assigned via the
     * flat crew list (or the leader field) at post/edit time - the same source of truth an
     * employee's own feed uses; Assignment rows only exist once hours have been logged, so they
     * can't tell us who was rostered but not yet logged. Drafts and cancelled jobs aren't rosters.
     */
    private List<Shift> shiftsFor(String employeeId, LocalDate from, LocalDate to) {
        java.util.Map<String, JobAd> rostered = new java.util.LinkedHashMap<>();
        jobAdRepository.findByLeaderId(employeeId).forEach(j -> rostered.put(j.getId(), j));
        jobAdCrewRepository.findByEmployeeId(employeeId)
                .forEach(c -> rostered.put(c.getJobAd().getId(), c.getJobAd()));

        List<Shift> shifts = new java.util.ArrayList<>();
        java.util.Set<String> logged = new java.util.HashSet<>();
        for (WorkHourEntry entry : workHourEntryRepository
                .findByAssignment_Employee_IdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, from, to)) {
            JobAd job = entry.getAssignment().getJobAd();
            logged.add(job.getId());
            shifts.add(new Shift(entry.getWorkDate(), job, entry.getHoursWorked()));
        }
        for (JobAd job : rostered.values()) {
            boolean inRange = !job.getWorkDate().isBefore(from) && !job.getWorkDate().isAfter(to);
            boolean live = job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.CANCELLED;
            if (inRange && live && !logged.contains(job.getId())) {
                shifts.add(new Shift(job.getWorkDate(), job, null));
            }
        }
        shifts.sort(java.util.Comparator.comparing(Shift::date));
        return shifts;
    }

    /** WORKED (hours logged), MISSING (a past job with no hours) or UPCOMING (today or later). */
    private static String shiftStatus(Shift shift, LocalDate today) {
        if (shift.hours() != null) {
            return "WORKED";
        }
        return shift.date().isBefore(today) ? "MISSING" : "UPCOMING";
    }

    /** One entry per calendar day in [from, to]: MISSING > WORKED > UPCOMING > OFF (no job that day). */
    private List<TimesheetDayResponse> dayStrip(List<Shift> shifts, LocalDate from, LocalDate to, LocalDate today) {
        List<TimesheetDayResponse> days = new java.util.ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            final LocalDate day = date;
            List<Shift> onDay = shifts.stream().filter(s -> s.date().equals(day)).toList();
            Double hours = onDay.stream().map(Shift::hours).filter(java.util.Objects::nonNull)
                    .reduce(Double::sum).orElse(null);
            String status;
            if (onDay.stream().anyMatch(s -> "MISSING".equals(shiftStatus(s, today)))) {
                status = "MISSING";
            } else if (hours != null) {
                status = "WORKED";
            } else if (!onDay.isEmpty()) {
                status = "UPCOMING";
            } else {
                status = "OFF";
            }
            days.add(new TimesheetDayResponse(day, status, hours));
        }
        return days;
    }

    private static double totalHoursOf(List<Shift> shifts) {
        return shifts.stream().filter(s -> s.hours() != null).mapToDouble(Shift::hours).sum();
    }

    @Override
    public List<TimesheetSummaryResponse> getTimesheets(String adminId, LocalDate from, LocalDate to) {
        Admin admin = findAdmin(adminId);
        LocalDate today = LocalDate.now();

        return employeeRepository.findByWorkspaceIdAndRemovedAtIsNull(admin.getWorkspace().getId()).stream()
                .map(employee -> {
                    List<Shift> shifts = shiftsFor(employee.getId(), from, to);
                    List<TimesheetDayResponse> days = dayStrip(shifts, from, to, today);
                    int loggedDays = (int) days.stream().filter(d -> d.getHoursWorked() != null).count();
                    int missingLogs = (int) days.stream().filter(d -> "MISSING".equals(d.getStatus())).count();
                    return new TimesheetSummaryResponse(
                            employee.getId(), employee.getFullName(), employee.getPhotoUrl(),
                            totalHoursOf(shifts), loggedDays, missingLogs, days);
                })
                .toList();
    }

    /**
     * A short prefix from the business name: an all-caps first word of up to three letters is
     * used as is ("MK Removals" gives "MK"), otherwise the initials of the first two words
     * ("Southern Cross Movers" gives "SC").
     */
    private static String codePrefix(String businessName) {
        String[] words = String.valueOf(businessName).trim().split("\\s+");
        if (words.length > 0 && words[0].length() >= 2 && words[0].length() <= 3
                && words[0].equals(words[0].toUpperCase()) && words[0].chars().allMatch(Character::isLetterOrDigit)) {
            return words[0];
        }
        StringBuilder initials = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0)) && initials.length() < 2) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.length() == 0 ? "ID" : initials.toString();
    }

    @Override
    public List<EmployeeOverviewResponse> getEmployeeOverview(String adminId, LocalDate from, LocalDate to) {
        Admin admin = findAdmin(adminId);
        String workspaceId = admin.getWorkspace().getId();
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        List<EmployeeOverviewResponse> result = new java.util.ArrayList<>();

        adminRepository.findByWorkspaceId(workspaceId).stream()
                .sorted(java.util.Comparator.comparing(a -> String.valueOf(a.getFullName()).toLowerCase()))
                .forEach(a -> result.add(new EmployeeOverviewResponse(
                        a.getId(), a.getFullName(), "Admin", a.getUsername(), a.getPhone(), a.getPhotoUrl(),
                        a.getCreatedAt(), null, 0, 0, null, 0, false, false, null, List.of(), List.of(), null)));

        employeeRepository.findByWorkspaceIdAndRemovedAtIsNull(workspaceId).stream()
                .sorted(java.util.Comparator.comparing(e -> String.valueOf(e.getFullName()).toLowerCase()))
                .forEach(e -> result.add(overviewOf(e, admin.getWorkspace(), from, to, today, now)));

        return result;
    }

    @Override
    public EmployeeOverviewResponse getMyOverview(String employeeId, LocalDate from, LocalDate to) {
        Employee employee = findEmployee(employeeId);
        if (employee.getRemovedAt() != null) {
            throw new ResourceNotFoundException("Ажилтан олдсонгүй: " + employeeId);
        }
        return overviewOf(employee, employee.getWorkspace(), from, to, LocalDate.now(), java.time.LocalTime.now());
    }

    /** One crew member's hours, pay, paid status and day-by-day breakdown for [from, to]. */
    private EmployeeOverviewResponse overviewOf(Employee e, Workspace workspace, LocalDate from, LocalDate to,
                                                LocalDate today, java.time.LocalTime now) {
        List<Shift> shifts = shiftsFor(e.getId(), from, to);
        List<TimesheetDayResponse> days = dayStrip(shifts, from, to, today);
        double totalHours = totalHoursOf(shifts);
        // Owed = what the hours are worth minus whatever was already marked paid; if more
        // hours get logged after a period was marked paid, the difference shows as owed again.
        double gross = e.getPayRate() == null
                ? 0
                : Math.round(totalHours * e.getPayRate() * 100.0) / 100.0;
        java.util.Optional<PayPeriodPayment> payment =
                payPeriodPaymentRepository.findByEmployeeIdAndPeriodStart(e.getId(), from);
        double owed = Math.max(0, Math.round((gross - payment.map(PayPeriodPayment::getAmount).orElse(0.0)) * 100.0) / 100.0);
        boolean paid = payment.isPresent() && owed == 0;
        List<TimesheetDayResponse> missing = days.stream()
                .filter(d -> "MISSING".equals(d.getStatus())).toList();

        // "On site" = rostered today, already started, and not yet logged off with hours.
        java.time.LocalTime onSiteSince = shiftsFor(e.getId(), today, today).stream()
                .filter(s -> s.hours() == null)
                .map(s -> s.jobAd().getStartTime())
                .filter(t -> t != null && !t.isAfter(now))
                .min(java.util.Comparator.naturalOrder())
                .orElse(null);

        List<ShiftResponse> shiftResponses = shifts.stream()
                .map(s -> new ShiftResponse(
                        s.date(), s.jobAd().getId(),
                        s.jobAd().getLocation() == null ? null : s.jobAd().getLocation().getAddressLine(),
                        s.hours(), shiftStatus(s, today), s.jobAd().getJobType(),
                        s.jobAd().getLeader() != null && s.jobAd().getLeader().getId().equals(e.getId())))
                .toList();

        return new EmployeeOverviewResponse(
                e.getId(), e.getFullName(), "Crew", e.getUsername(), e.getPhone(), e.getPhotoUrl(),
                e.getCreatedAt(), e.getPayRate(), totalHours, missing.size(),
                missing.isEmpty() ? null : missing.get(0).getDate(),
                owed, paid, onSiteSince != null,
                onSiteSince == null ? null : onSiteSince.toString().substring(0, 5),
                days, shiftResponses, codePrefix(workspace.getBusinessName()) + "-"
                        + String.format("%04d", userRepository.countByWorkspaceIdAndCreatedAtLessThan(
                                workspace.getId(), e.getCreatedAt()) + 1));
    }

    @Override
    @Transactional
    public void markPeriodPaid(String adminId, String employeeId, LocalDate from, LocalDate to) {
        Admin admin = findAdmin(adminId);
        Employee employee = findEmployee(employeeId);
        if (!employee.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new BadRequestException("Энэ ажилтан таны байгууллагад харьяалагдахгүй байна.");
        }
        double amount = employee.getPayRate() == null
                ? 0
                : Math.round(totalHoursOf(shiftsFor(employeeId, from, to)) * employee.getPayRate() * 100.0) / 100.0;

        // Re-marking after more hours were logged brings the paid amount up to date.
        PayPeriodPayment payment = payPeriodPaymentRepository.findByEmployeeIdAndPeriodStart(employeeId, from)
                .orElseGet(PayPeriodPayment::new);
        payment.setEmployee(employee);
        payment.setPeriodStart(from);
        payment.setPeriodEnd(to);
        payment.setAmount(amount);
        payment.setPaidBy(admin);
        payPeriodPaymentRepository.save(payment);
        activityLogService.log(adminId, "MARK_PERIOD_PAID:" + employeeId + "@" + from, null);
    }

    @Override
    @Transactional
    public void unmarkPeriodPaid(String adminId, String employeeId, LocalDate from) {
        Admin admin = findAdmin(adminId);
        Employee employee = findEmployee(employeeId);
        if (!employee.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new BadRequestException("Энэ ажилтан таны байгууллагад харьяалагдахгүй байна.");
        }
        payPeriodPaymentRepository.findByEmployeeIdAndPeriodStart(employeeId, from)
                .ifPresent(payPeriodPaymentRepository::delete);
        activityLogService.log(adminId, "UNMARK_PERIOD_PAID:" + employeeId + "@" + from, null);
    }

    @Override
    @Transactional
    public void removeEmployee(String adminId, String employeeId) {
        Admin admin = findAdmin(adminId);
        Employee employee = findEmployee(employeeId);
        if (!employee.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new BadRequestException("Энэ ажилтан таны байгууллагад харьяалагдахгүй байна.");
        }
        if (employee.getRemovedAt() != null) {
            return;
        }

        // Take them off jobs that have not happened yet; past jobs, hours and payments stay as history.
        LocalDate today = LocalDate.now();
        jobAdCrewRepository.findByEmployeeId(employeeId).stream()
                .filter(crew -> !crew.getJobAd().getWorkDate().isBefore(today))
                .forEach(jobAdCrewRepository::delete);
        jobAdRepository.findByLeaderId(employeeId).stream()
                .filter(job -> !job.getWorkDate().isBefore(today))
                .forEach(job -> {
                    job.setLeader(null);
                    jobAdRepository.save(job);
                });

        // Free up the email so the person can be invited again later.
        employee.setUsername(employee.getUsername() + "#removed-" + employee.getId().substring(0, 8));
        employee.setRemovedAt(java.time.LocalDateTime.now());
        employeeRepository.save(employee);
        activityLogService.log(adminId, "REMOVE_EMPLOYEE:" + employeeId, null);
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
