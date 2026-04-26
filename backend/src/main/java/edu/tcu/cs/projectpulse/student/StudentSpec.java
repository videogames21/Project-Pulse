package edu.tcu.cs.projectpulse.student;

import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public class StudentSpec {

    private StudentSpec() {}

    public static Specification<UserEntity> isStudent() {
        return (root, query, cb) -> cb.equal(root.get("role"), UserRole.STUDENT);
    }

    public static Specification<UserEntity> hasFirstName(String firstName) {
        return (root, query, cb) -> firstName == null || firstName.isBlank() ? null
                : cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
    }

    public static Specification<UserEntity> hasLastName(String lastName) {
        return (root, query, cb) -> lastName == null || lastName.isBlank() ? null
                : cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
    }

    public static Specification<UserEntity> hasEmail(String email) {
        return (root, query, cb) -> email == null || email.isBlank() ? null
                : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<UserEntity> teamIdIn(Collection<Long> teamIds) {
        return (root, query, cb) -> teamIds == null || teamIds.isEmpty() ? null
                : root.get("teamId").in(teamIds);
    }

    public static Specification<UserEntity> hasTeamId(Long teamId) {
        return (root, query, cb) -> teamId == null ? null
                : cb.equal(root.get("teamId"), teamId);
    }
}
