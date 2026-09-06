package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.WorkHourEntry;
import com.example.mk_backEnd.dto.AssignEmployeeRequest;
import com.example.mk_backEnd.dto.RecordWorkedHoursRequest;
import com.example.mk_backEnd.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admins/{adminId}")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private void verifySelf(Authentication authentication, String adminId) {
        if (!authentication.getName().equals(adminId)) {
            throw new AccessDeniedException("Өөр админы эрхээр хандах боломжгүй");
        }
    }

    @PostMapping("/job-ads/{jobAdId}/assignments")
    public ResponseEntity<Assignment> assignEmployee(Authentication authentication,
                                                      @PathVariable String adminId,
                                                      @PathVariable String jobAdId,
                                                      @Valid @RequestBody AssignEmployeeRequest request) {
        verifySelf(authentication, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.assignEmployee(adminId, jobAdId, request.getEmployeeId()));
    }

    @PutMapping("/job-ads/{jobAdId}/lead")
    public ResponseEntity<Assignment> setLead(Authentication authentication,
                                               @PathVariable String adminId,
                                               @PathVariable String jobAdId,
                                               @Valid @RequestBody AssignEmployeeRequest request) {
        verifySelf(authentication, adminId);
        return ResponseEntity.ok(adminService.setLead(adminId, jobAdId, request.getEmployeeId()));
    }

    @PostMapping("/work-hours")
    public ResponseEntity<WorkHourEntry> recordWorkedHours(Authentication authentication,
                                                            @PathVariable String adminId,
                                                            @Valid @RequestBody RecordWorkedHoursRequest request) {
        verifySelf(authentication, adminId);
        WorkHourEntry entry = adminService.recordWorkedHours(
                adminId, request.getAssignmentId(), request.getWorkDate(), request.getHoursWorked());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @GetMapping("/schedule")
    public ResponseEntity<List<Assignment>> viewSchedule(
            Authentication authentication,
            @PathVariable String adminId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        verifySelf(authentication, adminId);
        return ResponseEntity.ok(adminService.viewSchedule(from, to));
    }
}
