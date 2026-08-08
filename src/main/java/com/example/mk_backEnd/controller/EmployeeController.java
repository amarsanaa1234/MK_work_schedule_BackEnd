package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Address;
import com.example.mk_backEnd.domain.Assignment;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.service.EmployeeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees/{employeeId}")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    private void verifySelf(Authentication authentication, String employeeId) {
        if (!authentication.getName().equals(employeeId)) {
            throw new AccessDeniedException("Өөр ажилтны эрхээр хандах боломжгүй");
        }
    }

    @GetMapping("/job-ads")
    public ResponseEntity<List<JobAd>> viewMyJobAds(Authentication authentication, @PathVariable String employeeId) {
        verifySelf(authentication, employeeId);
        return ResponseEntity.ok(employeeService.viewMyJobAds(employeeId));
    }

    @GetMapping("/job-ads/{jobAdId}/location")
    public ResponseEntity<Address> viewJobLocation(Authentication authentication,
                                                    @PathVariable String employeeId, @PathVariable String jobAdId) {
        verifySelf(authentication, employeeId);
        return ResponseEntity.ok(employeeService.viewJobLocation(jobAdId));
    }

    @GetMapping("/job-ads/{jobAdId}/distance")
    public ResponseEntity<Map<String, Double>> distanceFromHome(Authentication authentication,
                                                                  @PathVariable String employeeId,
                                                                  @PathVariable String jobAdId) {
        verifySelf(authentication, employeeId);
        double km = employeeService.distanceFromHome(employeeId, jobAdId);
        return ResponseEntity.ok(Map.of("distanceKm", km));
    }

    @GetMapping("/schedule")
    public ResponseEntity<List<Assignment>> viewSchedule(
            Authentication authentication,
            @PathVariable String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        verifySelf(authentication, employeeId);
        return ResponseEntity.ok(employeeService.viewSchedule(employeeId, from, to));
    }
}
