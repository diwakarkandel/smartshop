package com.smartshop.config;

import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the initial SUPER_ADMIN account on first startup.
 * Enabled with app.bootstrap.enabled=true (e.g. BOOTSTRAP_ENABLED=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class BootstrapDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:admin@smartshop.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:Admin@12345}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-first-name:Super}")
    private String adminFirstName;

    @Value("${app.bootstrap.admin-last-name:Admin}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Bootstrap SUPER_ADMIN already exists, skipping.");
            return;
        }
        Role role = roleRepository.findByName(AppConstants.ROLE_SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "SUPER_ADMIN role not found. Flyway migrations must have seeded roles."));

        User user = new User();
        user.setFirstName(adminFirstName);
        user.setLastName(adminLastName);
        user.setEmail(adminEmail);
        user.setPasswordHash(passwordEncoder.encode(adminPassword));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        UserBranchRole assignment = new UserBranchRole();
        assignment.setUser(user);
        assignment.setShop(null);
        assignment.setBranch(null);
        assignment.setRole(role);
        assignment.setIsActive(true);
        userBranchRoleRepository.save(assignment);

        log.info("Bootstrap SUPER_ADMIN created: {}", adminEmail);
    }
}