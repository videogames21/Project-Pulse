package edu.tcu.cs.projectpulse.war;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WAREntryRepository extends JpaRepository<WAREntryEntity, Long> {
    List<WAREntryEntity> findByStudentIdAndWeekOrderByIdAsc(Long studentId, Integer week);
    List<WAREntryEntity> findByStudentIdAndWeekBetweenOrderByWeekAscIdAsc(Long studentId, Integer startWeek, Integer endWeek);
    List<WAREntryEntity> findByStudentIdInAndWeekOrderByStudentIdAscIdAsc(List<Long> studentIds, Integer week);
}
