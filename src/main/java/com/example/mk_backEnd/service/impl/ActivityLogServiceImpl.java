package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.ActivityLog;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.ActivityLogRepository;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.service.ActivityLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogServiceImpl(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ActivityLog log(String userId, String action, String ipAddress) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Хэрэглэгч олдсонгүй: " + userId));

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(user);
        activityLog.setAction(action);
        activityLog.setIpAddress(ipAddress);
        return activityLogRepository.save(activityLog);
    }

    @Override
    public List<ActivityLog> findByUser(String userId) {
        return activityLogRepository.findByUserId(userId);
    }
}
