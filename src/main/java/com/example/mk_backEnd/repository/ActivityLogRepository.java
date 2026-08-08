package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {

    List<ActivityLog> findByUserId(String userId);
}
