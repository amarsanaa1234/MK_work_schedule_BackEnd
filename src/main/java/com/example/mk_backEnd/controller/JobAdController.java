package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.service.JobAdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-ads")
public class JobAdController {

    private final JobAdService jobAdService;

    public JobAdController(JobAdService jobAdService) {
        this.jobAdService = jobAdService;
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
