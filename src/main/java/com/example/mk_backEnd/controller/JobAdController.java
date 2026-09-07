package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.dto.CreateJobAdRequest;
import com.example.mk_backEnd.service.AdminService;
import com.example.mk_backEnd.service.JobAdService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-ads")
public class JobAdController {

    private final JobAdService jobAdService;
    private final AdminService adminService;

    public JobAdController(JobAdService jobAdService, AdminService adminService) {
        this.jobAdService = jobAdService;
        this.adminService = adminService;
    }

    @PostMapping("/job-create-post")
    public ResponseEntity<JobAd> create(Authentication authentication,
                                         @Valid @RequestBody CreateJobAdRequest request) {
        String adminId = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createJobAd(adminId, request));
    }

    @GetMapping
    public ResponseEntity<List<JobAd>> findAll() {
        return ResponseEntity.ok(jobAdService.findAll());
    }

    @GetMapping("/{jobAdId}")
    public ResponseEntity<JobAd> findById(@PathVariable String jobAdId) {
        return ResponseEntity.ok(jobAdService.findById(jobAdId));
    }

    @DeleteMapping("/{jobAdId}")
    public ResponseEntity<Void> delete(@PathVariable String jobAdId) {
        jobAdService.delete(jobAdId);
        return ResponseEntity.noContent().build();
    }
}
