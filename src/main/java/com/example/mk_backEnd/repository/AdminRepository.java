package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, String> {
}
