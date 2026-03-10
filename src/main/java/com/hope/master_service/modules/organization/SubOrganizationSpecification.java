package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.SubOrganizationType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SubOrganizationSpecification {

    private SubOrganizationSpecification() {
    }

    public static Specification<SubOrganizationEntity> withFilters(
            Long organizationId,
            String search,
            Boolean active,
            List<SubOrganizationType> types,
            String createdBy,
            Instant createdFrom,
            Instant createdTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            predicates.add(cb.isFalse(root.get("archive")));

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            } else {
                // Default: show active only
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
                predicates.add(cb.or(nameMatch, codeMatch));
            }

            if (types != null && !types.isEmpty()) {
                predicates.add(root.get("type").in(types));
            }

            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }

            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("created"), createdFrom));
            }

            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("created"), createdTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
