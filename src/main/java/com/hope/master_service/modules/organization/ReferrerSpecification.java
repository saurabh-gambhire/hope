package com.hope.master_service.modules.organization;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReferrerSpecification {

    private ReferrerSpecification() {
    }

    public static Specification<ReferrerEntity> withFilters(Long organizationId, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            predicates.add(cb.isFalse(root.get("archive")));
            predicates.add(cb.isTrue(root.get("active")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate fullNameMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                        pattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, fullNameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
