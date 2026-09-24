package com.kangoute.appointment.config;

import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private final DemoProperties demoProperties;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DemoProperties.Account demoUser = demoProperties.getUser();
        DemoProperties.Account demoAdmin = demoProperties.getAdmin();
        validate(demoUser, demoAdmin);
        createIfAbsent(demoUser, RoleName.ROLE_USER, DemoAccountType.USER);
        createIfAbsent(demoAdmin, RoleName.ROLE_ADMIN, DemoAccountType.ADMIN);
    }

    private void validate(DemoProperties.Account demoUser, DemoProperties.Account demoAdmin) {
        if (isBlank(demoUser.getEmail()) || isBlank(demoUser.getPassword())
                || isBlank(demoAdmin.getEmail()) || isBlank(demoAdmin.getPassword())
                || isBlank(demoUser.getFirstName()) || isBlank(demoUser.getLastName())
                || isBlank(demoAdmin.getFirstName()) || isBlank(demoAdmin.getLastName())) {
            throw new IllegalStateException("Demo accounts require an email, password, first name and last name");
        }
        if (demoUser.getEmail().equalsIgnoreCase(demoAdmin.getEmail())) {
            throw new IllegalStateException("Demo user and admin must have different emails");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void createIfAbsent(DemoProperties.Account account, RoleName role, DemoAccountType type) {
        List<User> markedAccounts = userRepository.findByDemoAccountType(type);
        if (markedAccounts.size() > 1) {
            throw new IllegalStateException("Multiple accounts have the same demo account type");
        }
        if (!markedAccounts.isEmpty()) {
            return; // Preserve the marked account even when the configured email changes.
        }
        if (userRepository.existsByEmail(account.getEmail())) {
            throw new IllegalStateException("Configured demo email belongs to an existing non-demo account");
        }
        User user = new User();
        user.setEmail(account.getEmail());
        user.setFirstName(account.getFirstName());
        user.setLastName(account.getLastName());
        user.setPassword(passwordEncoder.encode(account.getPassword()));
        user.setDemoAccountType(type);
        user.setRoles(new HashSet<>(Set.of(roleService.createRole(role))));
        userRepository.save(user);
    }
}
