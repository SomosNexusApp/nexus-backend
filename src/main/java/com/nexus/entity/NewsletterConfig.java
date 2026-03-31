package com.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "newsletter_config")
public class NewsletterConfig extends DomainEntity {

    @Column(nullable = false)
    private boolean automatedEnabled = false;

    @Column(nullable = false)
    private int dayOfWeek = 1; // 1 = Monday, 7 = Sunday

    @Column(nullable = false)
    private LocalTime timeOfDay = LocalTime.of(10, 0);

    private LocalDateTime lastSent;

    // Getters and Setters
    public boolean isAutomatedEnabled() { return automatedEnabled; }
    public void setAutomatedEnabled(boolean automatedEnabled) { this.automatedEnabled = automatedEnabled; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(LocalTime timeOfDay) { this.timeOfDay = timeOfDay; }

    public LocalDateTime getLastSent() { return lastSent; }
    public void setLastSent(LocalDateTime lastSent) { this.lastSent = lastSent; }
}
