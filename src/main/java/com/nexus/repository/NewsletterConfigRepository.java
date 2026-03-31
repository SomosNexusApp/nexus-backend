package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nexus.entity.NewsletterConfig;

@Repository
public interface NewsletterConfigRepository extends JpaRepository<NewsletterConfig, Integer> {
}
