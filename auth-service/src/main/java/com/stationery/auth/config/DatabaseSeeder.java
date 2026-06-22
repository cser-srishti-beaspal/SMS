package com.stationery.auth.config;

import com.stationery.auth.model.Role;
import com.stationery.auth.model.User;
import com.stationery.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeder to initialize default users in the database on startup.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database seeding...");

        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@stationery.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user (username: admin, password: admin123)");
        }

        if (!userRepository.existsByUsername("student")) {
            User student = User.builder()
                    .username("student")
                    .email("student@stationery.com")
                    .password(passwordEncoder.encode("student123"))
                    .role(Role.STUDENT)
                    .build();
            userRepository.save(student);
            log.info("Seeded default student user (username: student, password: student123)");
        }

        log.info("Database seeding completed.");
    }
}
