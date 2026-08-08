package com.example.mk_backEnd.repository;

import com.example.mk_backEnd.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, String> {
}
