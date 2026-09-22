package com.kangoute.appointment;

import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.service.RoleService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserUpdateIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void updateUserWithoutPasswordKeepsExistingPassword() {
        User user = createUser("admin-update-no-password@example.com");
        String existingPassword = user.getPassword();

        User update = new User();
        update.setFirstName("Updated");
        update.setLastName("User");
        update.setEmail("admin-update-no-password-renamed@example.com");
        update.setPassword(null);
        update.setRoles(new HashSet<>(Set.of(roleService.createRole(RoleName.ROLE_ADMIN))));

        User saved = userService.updateUser(user.getId(), update);

        assertEquals(existingPassword, saved.getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.getPassword()));
        assertEquals("admin-update-no-password-renamed@example.com", saved.getEmail());
        assertTrue(saved.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN));
    }

    @Test
    void updateUserWithBlankPasswordKeepsExistingPassword() {
        User user = createUser("admin-update-blank-password@example.com");
        String existingPassword = user.getPassword();

        User update = new User();
        update.setFirstName("Blank");
        update.setLastName("Password");
        update.setEmail(user.getEmail());
        update.setPassword("   ");
        update.setRoles(new HashSet<>(Set.of(roleService.createRole(RoleName.ROLE_USER))));

        User saved = userService.updateUser(user.getId(), update);

        assertEquals(existingPassword, saved.getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.getPassword()));
    }

    @Test
    void updateUserWithNewPasswordEncodesAndPersistsIt() {
        User user = createUser("admin-update-new-password@example.com");
        String existingPassword = user.getPassword();

        User update = new User();
        update.setFirstName(user.getFirstName());
        update.setLastName(user.getLastName());
        update.setEmail(user.getEmail());
        update.setPassword("newSecret123");
        update.setRoles(new HashSet<>(Set.of(roleService.createRole(RoleName.ROLE_USER))));

        User saved = userService.updateUser(user.getId(), update);

        assertTrue(passwordEncoder.matches("newSecret123", saved.getPassword()));
        assertTrue(!existingPassword.equals(saved.getPassword()));
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }
}
