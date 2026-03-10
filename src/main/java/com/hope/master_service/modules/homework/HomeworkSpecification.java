package com.hope.master_service.modules.homework;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeworkSpecification {

    private HomeworkSpecification() {
    }

    public static Specification<HomeworkEntity> withFilters(
            String search,
            UUID subOrganizationUuid,
            UUID contractUuid,
            Boolean active) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("archive")));

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            } else {
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                predicates.add(nameMatch);
            }

            if (subOrganizationUuid != null) {
                predicates.add(cb.equal(root.get("subOrganization").get("uuid"), subOrganizationUuid));
            }

            if (contractUuid != null) {
                predicates.add(cb.equal(root.get("contract").get("uuid"), contractUuid));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
