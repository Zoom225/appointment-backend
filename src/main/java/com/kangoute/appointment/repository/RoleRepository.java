package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.Role;
import com.kangoute.appointment.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);

}
