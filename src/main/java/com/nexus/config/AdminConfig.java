package com.nexus.config;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_config")
public class AdminConfig {

    @Id
    @Column(name = "config_key", unique = true, nullable = false)
    private String key;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;

    public AdminConfig() {}

    public AdminConfig(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
