package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.enums.ContractStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ContractSpecification {

    private ContractSpecification() {
    }

    public static Specification<ContractEntity> withFilters(
            Long organizationId,
            String search,
            List<ContractStatus> statuses,
            Boolean active,
            Boolean isTemplate,
            String createdBy,
            Instant createdFrom,
            Instant createdTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            predicates.add(cb.isFalse(root.get("archive")));

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (isTemplate != null) {
                predicates.add(cb.equal(root.get("isTemplate"), isTemplate));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate identifierMatch = cb.like(cb.lower(root.get("identifier")), pattern);
                Predicate typeMatch = cb.like(cb.lower(root.get("contractType")), pattern);
                predicates.add(cb.or(nameMatch, identifierMatch, typeMatch));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
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
