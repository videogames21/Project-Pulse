package edu.tcu.cs.projectpulse.rubric;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RubricRepository extends JpaRepository<RubricEntity, Long> {
    boolean existsByName(String name);

    @EntityGraph(attributePaths = {"criteria"})
    List<RubricEntity> findAll();

    @EntityGraph(attributePaths = {"criteria"})
    Optional<RubricEntity> findById(Long id);
}
