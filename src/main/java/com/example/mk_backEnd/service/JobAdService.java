package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.JobAd;

import java.util.List;

public interface JobAdService {

    List<JobAd> findAll();

    JobAd findById(String jobAdId);

    void delete(String jobAdId);
}
