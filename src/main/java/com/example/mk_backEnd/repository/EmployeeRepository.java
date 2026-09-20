package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, String> {

    // SINGLE_TABLE inheritance-ийн ачаар энэ query автоматаар зөвхөн
    // user_type='EMPLOYEE' мөрүүдийг л буцаана (Admin-ууд оролцохгүй).
    List<Employee> findByWorkspaceId(String workspaceId);

    long countByWorkspaceId(String workspaceId);
}
