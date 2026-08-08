package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.ActivityLog;

import java.util.List;

public interface ActivityLogService {

    ActivityLog log(String userId, String action, String ipAddress);

    List<ActivityLog> findByUser(String userId);
}
