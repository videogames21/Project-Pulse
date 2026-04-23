package edu.tcu.cs.projectpulse.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RubricRepository extends JpaRepository<RubricEntity, Long> {
    boolean existsByName(String name);
    Optional<RubricEntity> findByActiveTrue();
}
