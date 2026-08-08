package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
}
