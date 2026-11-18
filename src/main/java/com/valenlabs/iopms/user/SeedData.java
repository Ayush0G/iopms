package com.valenlabs.iopms.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin so the system can be used immediately after startup. */
@Component
@RequiredArgsConstructor
public class SeedData implements CommandLineRunner {

    static final String ADMIN_EMAIL = "admin@valenlabs.com";
    static final String ADMIN_PASSWORD = "Admin@123";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            User admin = new User();
            admin.setName("Valen Labs Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(encoder.encode(ADMIN_PASSWORD));
            admin.setRole(Role.ADMIN);
            users.save(admin);
        }
    }
}
