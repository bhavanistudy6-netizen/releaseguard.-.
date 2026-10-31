package com.releaseguard.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class SecurityDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.initial-admin.password:${SECURITY_INITIAL_ADMIN_PASSWORD:${INITIAL_ADMIN_PASSWORD:}}}")
    private String initialAdminPassword;

    @Value("${security.initial-admin.username:${SECURITY_INITIAL_ADMIN_USERNAME:admin}}")
    private String initialAdminUsername;

    @Value("${security.seed-test-accounts:${SECURITY_SEED_TEST_ACCOUNTS:false}}")
    private boolean seedTestAccounts;

    @Value("${security.test-accounts.password:${SECURITY_TEST_ACCOUNTS_PASSWORD:}}")
    private String testAccountsPassword;

    public SecurityDataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapAdminIfNeeded();
        seedTestAccountsIfNeeded();
    }

    private void bootstrapAdminIfNeeded() {
        long activeAdmins = userRepository.countActiveAdmins();
        if (activeAdmins > 0) {
            logger.debug("Active administrator account already exists. Skipping admin bootstrap.");
            return;
        }

        if (initialAdminPassword != null && !initialAdminPassword.isBlank()) {
            String username = initialAdminUsername != null && !initialAdminUsername.isBlank()
                    ? initialAdminUsername.trim().toLowerCase()
                    : "admin";

            if (!userRepository.existsByUsername(username)) {
                String hash = passwordEncoder.encode(initialAdminPassword);
                User admin = new User(username, hash, "System Administrator", Set.of(Role.ADMIN));
                userRepository.save(admin);
                logger.info("Security bootstrap: Initial administrator account '{}' provisioned successfully.", username);
            }
        } else {
            logger.warn("Security bootstrap: No active administrator account found. Set INITIAL_ADMIN_PASSWORD in environment to bootstrap.");
        }
    }

    private void seedTestAccountsIfNeeded() {
        if (!seedTestAccounts) {
            return;
        }

        if (testAccountsPassword == null || testAccountsPassword.isBlank()) {
            logger.warn("Test account seeding requested, but SECURITY_TEST_ACCOUNTS_PASSWORD was not provided. Skipping test accounts.");
            return;
        }

        String hash = passwordEncoder.encode(testAccountsPassword);

        createTestAccountIfMissing("viewer", hash, "Demo Viewer", Set.of(Role.VIEWER));
        createTestAccountIfMissing("developer", hash, "Demo Developer", Set.of(Role.DEVELOPER));
        createTestAccountIfMissing("reviewer", hash, "Demo Reviewer", Set.of(Role.REVIEWER));
        createTestAccountIfMissing("deployer", hash, "Demo Deployer", Set.of(Role.DEPLOYER));

        logger.info("Security bootstrap: Test accounts seeded successfully.");
    }

    private void createTestAccountIfMissing(String username, String passwordHash, String displayName, Set<Role> roles) {
        if (!userRepository.existsByUsername(username.toLowerCase())) {
            User user = new User(username.toLowerCase(), passwordHash, displayName, roles);
            userRepository.save(user);
        }
    }
}

