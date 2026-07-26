package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.Role;
import com.kangoute.appointment.enums.RoleName;

public interface RoleService {

    Role createRole(RoleName roleName);

    Role getRole(RoleName roleName);

    boolean exists(RoleName roleName);
}
