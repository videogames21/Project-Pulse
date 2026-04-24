package edu.tcu.cs.projectpulse.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TeamRepository extends JpaRepository<TeamEntity, Long>, JpaSpecificationExecutor<TeamEntity> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<TeamEntity> findAllBySectionNameOrderByNameAsc(String sectionName);
}
