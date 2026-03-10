package com.hope.master_service.modules.organization;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrganizationSpecification {

    private OrganizationSpecification() {
    }

    public static Specification<OrganizationEntity> withFilters(String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only show non-archived
            predicates.add(cb.isFalse(root.get("archive")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate abbreviationMatch = cb.like(cb.lower(root.get("abbreviation")), pattern);
                predicates.add(cb.or(nameMatch, abbreviationMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
