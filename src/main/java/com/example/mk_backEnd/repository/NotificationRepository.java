package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String employeeId);

    List<Notification> findByRecipientIdAndReadFalse(String employeeId);
}
