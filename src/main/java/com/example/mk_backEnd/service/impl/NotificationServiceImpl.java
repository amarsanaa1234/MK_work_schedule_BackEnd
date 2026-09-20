package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.JobAd;
import com.example.mk_backEnd.domain.Notification;
import com.example.mk_backEnd.dto.NotificationResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.JobAdCrewRepository;
import com.example.mk_backEnd.repository.NotificationRepository;
import com.example.mk_backEnd.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JobAdCrewRepository jobAdCrewRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                    JobAdCrewRepository jobAdCrewRepository) {
        this.notificationRepository = notificationRepository;
        this.jobAdCrewRepository = jobAdCrewRepository;
    }

    @Override
    @Transactional
    public void notifyCrew(JobAd jobAd, String message) {
        Set<Employee> recipients = new LinkedHashSet<>();
        if (jobAd.getLeader() != null) {
            recipients.add(jobAd.getLeader());
        }
        jobAdCrewRepository.findByJobAdId(jobAd.getId()).forEach(crew -> recipients.add(crew.getEmployee()));

        for (Employee recipient : recipients) {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setJobAd(jobAd);
            notificationRepository.save(notification);
        }
    }

    @Override
    public List<NotificationResponse> listForEmployee(String employeeId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId).stream()
                .map(n -> new NotificationResponse(
                        n.getId(), n.getMessage(), n.getJobAd() == null ? null : n.getJobAd().getId(),
                        n.getCreatedAt(), n.isRead()))
                .toList();
    }

    @Override
    @Transactional
    public void markRead(String employeeId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Мэдэгдэл олдсонгүй: " + notificationId));
        if (!notification.getRecipient().getId().equals(employeeId)) {
            throw new BadRequestException("Энэ мэдэгдэл танд харьяалагдахгүй байна.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(String employeeId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalse(employeeId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
