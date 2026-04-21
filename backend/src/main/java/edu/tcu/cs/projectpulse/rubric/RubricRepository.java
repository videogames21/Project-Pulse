package edu.tcu.cs.projectpulse.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RubricRepository extends JpaRepository<RubricEntity, Long> {
    boolean existsByName(String name);
}
