package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Admin;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer>{
  java.util.Optional<com.nexus.entity.Admin> findByEmail(String email);
}
