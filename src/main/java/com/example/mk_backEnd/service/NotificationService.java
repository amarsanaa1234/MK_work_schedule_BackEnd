package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    /** Notifies the job's leader and every crew member with the given message. */
    void notifyCrew(JobAd jobAd, String message);

    List<NotificationResponse> listForEmployee(String employeeId);

    void markRead(String employeeId, String notificationId);

    void markAllRead(String employeeId);
}
