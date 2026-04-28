package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nexus.config.AdminConfig;

@Repository
public interface AdminConfigRepository extends JpaRepository<AdminConfig, String> {
}
