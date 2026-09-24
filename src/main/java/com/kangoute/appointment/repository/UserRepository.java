package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByDemoAccountType(DemoAccountType demoAccountType);

    List<User> findDistinctByRolesNameAndDemoAccountType(
            RoleName role, DemoAccountType demoAccountType);
}
