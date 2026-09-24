package com.kangoute.appointment;

import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.service.RoleService;
import com.kangoute.appointment.service.UserService;
import com.kangoute.appointment.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-data-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "app.demo.enabled=false"
})
@Transactional
class DemoDataInitializerDisabledTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired private UserService userService;
    @Autowired private RoleService roleService;
    @Autowired private CustomUserDetailsService userDetailsService;

    @Test
    void demoDataIsNotCreatedWhenDisabled() {
        assertTrue(userRepository.findByEmail("demo.user@appointment.local").isEmpty());
        assertTrue(userRepository.findByEmail("demo.admin@appointment.local").isEmpty());
    }

    @Test
    void previouslyCreatedDemoAdminCannotAuthenticateWhenDemoIsDisabled() {
        User account = new User();
        account.setFirstName("Demo");
        account.setLastName("Administrateur");
        account.setEmail("demo.admin@appointment.local");
        account.setPassword("TestOnly2026!");
        User saved = userService.createUser(account);
        saved.setDemoAccountType(DemoAccountType.ADMIN);
        saved.setRoles(new HashSet<>(Set.of(roleService.createRole(RoleName.ROLE_ADMIN))));
        userRepository.saveAndFlush(saved);

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("demo.admin@appointment.local"));
    }
}
