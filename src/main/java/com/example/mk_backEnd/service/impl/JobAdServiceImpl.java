package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.JobAdRepository;
import com.example.mk_backEnd.service.JobAdService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobAdServiceImpl implements JobAdService {

    private final JobAdRepository jobAdRepository;

    public JobAdServiceImpl(JobAdRepository jobAdRepository) {
        this.jobAdRepository = jobAdRepository;
    }

    @Override
    public List<JobAd> findAll() {
        return jobAdRepository.findAll();
    }

    @Override
    public JobAd findById(String jobAdId) {
        return jobAdRepository.findById(jobAdId)
                .orElseThrow(() -> new ResourceNotFoundException("Ажлын зар олдсонгүй: " + jobAdId));
    }

    @Override
    public void delete(String jobAdId) {
        if (!jobAdRepository.existsById(jobAdId)) {
            throw new ResourceNotFoundException("Ажлын зар олдсонгүй: " + jobAdId);
        }
        jobAdRepository.deleteById(jobAdId);
    }
}
