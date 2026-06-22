package com.stationery.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the Auth Service.
 * Handles user registration, login, and JWT token management.
 */
@SpringBootApplication
@EnableDiscoveryClient
// discovery client activate karo
// service registry ke saath connect karo
// service ko register karo
// dusri services ko discover karne ki capability do
public class AuthServiceApplication {

    public static void main(String[] args) {
        //static method : Matlab is method ko call karne ke liye object banane ki zarurat nahi.
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
