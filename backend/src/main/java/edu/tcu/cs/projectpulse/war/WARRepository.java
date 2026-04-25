package edu.tcu.cs.projectpulse.war;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WARRepository extends JpaRepository<WAREntity, Long> {

    Optional<WAREntity> findByStudentIdAndWeekStart(Long studentId, LocalDate weekStart);

    List<WAREntity> findAllByStudentId(Long studentId);

    List<WAREntity> findAllByStudentIdAndWeekStartBetween(Long studentId, LocalDate startWeek, LocalDate endWeek);
}
