package com.hope.master_service.modules.provider;

import com.hope.master_service.dto.enums.ProviderType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.enums.UserStatus;
import com.hope.master_service.modules.user.UserEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProviderSpecification {

    private ProviderSpecification() {
    }

    public static Specification<ProviderEntity> withFilters(
            String search,
            UserStatus status,
            List<Roles> roles,
            ProviderType providerType,
            Instant lastLoginFrom,
            Instant lastLoginTo,
            Boolean neverLoggedIn) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<ProviderEntity, UserEntity> user = root.join("user");

            addStatusPredicate(predicates, user, cb, status);

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(user.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(user.get("lastName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(user.get("email")), pattern);
                Predicate fullNameMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(user.get("firstName"), " "), user.get("lastName"))),
                        pattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch, fullNameMatch));
            }

            if (roles != null && !roles.isEmpty()) {
                predicates.add(user.get("role").in(roles));
            }

            if (providerType != null) {
                predicates.add(cb.equal(root.get("providerType"), providerType));
            }

            if (lastLoginFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(user.get("lastLogin"), lastLoginFrom));
            }
            if (lastLoginTo != null) {
                predicates.add(cb.lessThanOrEqualTo(user.get("lastLogin"), lastLoginTo));
            }

            if (Boolean.TRUE.equals(neverLoggedIn)) {
                predicates.add(cb.isNull(user.get("lastLogin")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addStatusPredicate(List<Predicate> predicates,
                                           Join<ProviderEntity, UserEntity> user,
                                           CriteriaBuilder cb, UserStatus status) {
        if (status == null || status == UserStatus.ACTIVE) {
            predicates.add(cb.isTrue(user.get("active")));
            predicates.add(cb.isFalse(user.get("archive")));
            predicates.add(cb.isTrue(user.get("emailVerified")));
        } else if (status == UserStatus.INACTIVE) {
            predicates.add(cb.isFalse(user.get("active")));
            predicates.add(cb.isFalse(user.get("archive")));
        } else if (status == UserStatus.PENDING) {
            predicates.add(cb.isFalse(user.get("emailVerified")));
            predicates.add(cb.isFalse(user.get("archive")));
        } else if (status == UserStatus.SUSPENDED) {
            predicates.add(cb.isTrue(user.get("archive")));
        }
    }

    public static Specification<ProviderEntity> isActive() {
        return (root, query, cb) -> {
            Join<ProviderEntity, UserEntity> user = root.join("user");
            return cb.and(
                    cb.isTrue(user.get("active")),
                    cb.isFalse(user.get("archive")),
                    cb.isTrue(user.get("emailVerified"))
            );
        };
    }

    public static Specification<ProviderEntity> isInactive() {
        return (root, query, cb) -> {
            Join<ProviderEntity, UserEntity> user = root.join("user");
            return cb.and(
                    cb.isFalse(user.get("active")),
                    cb.isFalse(user.get("archive"))
            );
        };
    }

    public static Specification<ProviderEntity> isPending() {
        return (root, query, cb) -> {
            Join<ProviderEntity, UserEntity> user = root.join("user");
            return cb.and(
                    cb.isFalse(user.get("emailVerified")),
                    cb.isFalse(user.get("archive"))
            );
        };
    }

    public static Specification<ProviderEntity> isSuspended() {
        return (root, query, cb) -> {
            Join<ProviderEntity, UserEntity> user = root.join("user");
            return cb.isTrue(user.get("archive"));
        };
    }
}
