package edu.tcu.cs.projectpulse.team;

import org.springframework.data.jpa.domain.Specification;

public class TeamSpec {

    public static Specification<TeamEntity> hasSectionName(String sectionName) {
        return (root, query, cb) ->
                sectionName == null ? null : cb.like(cb.lower(root.get("sectionName")), "%" + sectionName.toLowerCase() + "%");
    }

    public static Specification<TeamEntity> hasTeamName(String teamName) {
        return (root, query, cb) ->
                teamName == null ? null : cb.like(cb.lower(root.get("name")), "%" + teamName.toLowerCase() + "%");
    }
}
