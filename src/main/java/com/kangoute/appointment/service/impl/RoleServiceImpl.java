package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.entity.Role;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.RoleRepository;
import com.kangoute.appointment.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role createRole(RoleName roleName) {
        if (roleRepository.existsByName(roleName)) {
            return roleRepository.findByName(roleName).orElseThrow();
        }

        Role role = Role.builder()
                .name(roleName)
                .build();

        return roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(RoleName roleName) {
        return roleRepository.existsByName(roleName);
    }
}
