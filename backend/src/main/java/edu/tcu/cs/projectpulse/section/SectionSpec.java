package edu.tcu.cs.projectpulse.section;

import org.springframework.data.jpa.domain.Specification;

public class SectionSpec {

    public static Specification<SectionEntity> hasName(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
