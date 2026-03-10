package com.hope.master_service.modules.user;

import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.enums.UserStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<UserEntity> withFilters(
            String search,
            UserStatus status,
            List<Roles> roles,
            Instant lastLoginFrom,
            Instant lastLoginTo,
            Boolean neverLoggedIn) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter (default: ACTIVE)
            addStatusPredicate(predicates, root, cb, status);

            // Search by name or email (case-insensitive, partial match)
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate fullNameMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                        pattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch, fullNameMatch));
            }

            // Role filter (multi-select)
            if (roles != null && !roles.isEmpty()) {
                predicates.add(root.get("role").in(roles));
            }

            // Last login date range filter
            if (lastLoginFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastLogin"), lastLoginFrom));
            }
            if (lastLoginTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastLogin"), lastLoginTo));
            }

            // Never logged in filter
            if (Boolean.TRUE.equals(neverLoggedIn)) {
                predicates.add(cb.isNull(root.get("lastLogin")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addStatusPredicate(List<Predicate> predicates, Root<UserEntity> root,
                                           CriteriaBuilder cb, UserStatus status) {
        if (status == null || status == UserStatus.ACTIVE) {
            // Active: active=true, archive=false, emailVerified=true
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isFalse(root.get("archive")));
            predicates.add(cb.isTrue(root.get("emailVerified")));
        } else if (status == UserStatus.INACTIVE) {
            // Inactive: active=false, archive=false
            predicates.add(cb.isFalse(root.get("active")));
            predicates.add(cb.isFalse(root.get("archive")));
        } else if (status == UserStatus.PENDING) {
            // Pending: emailVerified=false, archive=false
            predicates.add(cb.isFalse(root.get("emailVerified")));
            predicates.add(cb.isFalse(root.get("archive")));
        } else if (status == UserStatus.SUSPENDED) {
            // Suspended: archive=true
            predicates.add(cb.isTrue(root.get("archive")));
        }
        // ALL: no status filter
    }

    public static Specification<UserEntity> isActive() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.isFalse(root.get("archive")),
                cb.isTrue(root.get("emailVerified"))
        );
    }

    public static Specification<UserEntity> isInactive() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("active")),
                cb.isFalse(root.get("archive"))
        );
    }

    public static Specification<UserEntity> isPending() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("emailVerified")),
                cb.isFalse(root.get("archive"))
        );
    }

    public static Specification<UserEntity> isSuspended() {
        return (root, query, cb) -> cb.isTrue(root.get("archive"));
    }
}
