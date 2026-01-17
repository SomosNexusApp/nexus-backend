package com.nexus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexusApplication implements CommandLineRunner {

    @Autowired
    private PopulateDB populateDB;

    public static void main(String[] args) {
        SpringApplication.run(NexusApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        populateDB.popular();
    }

}