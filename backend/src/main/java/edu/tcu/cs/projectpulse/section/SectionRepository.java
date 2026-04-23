package edu.tcu.cs.projectpulse.section;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SectionRepository extends JpaRepository<SectionEntity, Long>, JpaSpecificationExecutor<SectionEntity> {
    boolean existsByName(String name);
}
