package org.serviceproject.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.entity.UserRole;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstrap admin account creation on first startup.
 * <p>
 * If no GENERAL_ADMIN user exists and {@code ADMIN_PHONE} + {@code ADMIN_PASSWORD}
 * environment variables are provided, creates a bootstrap admin account.
 * <p>
 * Idempotent: skips if any admin already exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.serviceproject.academic.service.AcademicYearService academicYearService;
    private final org.serviceproject.common.config.AppProperties appProperties;

    @Value("${admin.phone:#{null}}")
    private String adminPhone;

    @Value("${admin.password:#{null}}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        // Ensure academic year exists
        try {
            java.time.LocalDate today = org.serviceproject.common.util.DateUtil.today(appProperties.timeZone());
            academicYearService.ensureAcademicYear(today);
            log.info("Ensured current academic year on startup");
        } catch (Exception e) {
            log.warn("Could not auto-ensure academic year on startup: {}", e.getMessage());
        }

        if (adminPhone == null || adminPhone.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("No ADMIN_PHONE/ADMIN_PASSWORD env vars set — skipping bootstrap admin creation");
            return;
        }

        if (userAccountRepository.existsAnyAdmin()) {
            log.info("Admin account already exists — skipping bootstrap");
            return;
        }

        if (personRepository.existsByPhone(adminPhone)) {
            log.warn("Phone {} already in use — cannot create bootstrap admin", adminPhone);
            return;
        }

        Person person = new Person();
        person.setFullName("System Admin");
        person.setPhone(adminPhone.trim());
        person.setGender(Gender.MALE);
        personRepository.save(person);

        UserAccount account = new UserAccount();
        account.setPerson(person);
        account.setPassword(passwordEncoder.encode(adminPassword));
        account.setEnabled(true);

        UserRole adminRole = new UserRole(Role.GENERAL_ADMIN);
        account.addRole(adminRole);

        userAccountRepository.save(account);

        log.info("Bootstrap GENERAL_ADMIN account created for phone: {}", adminPhone);
    }
}
