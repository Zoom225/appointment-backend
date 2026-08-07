package com.kangoute.appointment.repository.specification;

import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> matchesQuery(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String like = "%" + query.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), like),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), like),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), like)
            );
        };
    }

    public static Specification<User> hasRole(RoleName roleName) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (roleName == null) {
                return criteriaBuilder.conjunction();
            }

            criteriaQuery.distinct(true);
            var roleJoin = root.join("roles");
            return criteriaBuilder.equal(roleJoin.get("name"), roleName);
        };
    }
}
