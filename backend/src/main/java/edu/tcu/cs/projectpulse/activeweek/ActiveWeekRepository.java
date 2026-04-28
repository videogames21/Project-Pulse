package edu.tcu.cs.projectpulse.activeweek;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ActiveWeekRepository extends JpaRepository<ActiveWeekEntity, Long> {

    List<ActiveWeekEntity> findBySectionIdOrderByWeekStartDateAsc(Long sectionId);

    @Modifying
    @Query("DELETE FROM ActiveWeekEntity a WHERE a.sectionId = :sectionId")
    void deleteBySectionId(Long sectionId);

    @Modifying
    @Query("DELETE FROM ActiveWeekEntity a WHERE a.sectionId = :sectionId " +
           "AND (a.weekStartDate < :startDate OR a.weekStartDate > :endDate)")
    void deleteOutOfRange(Long sectionId, LocalDate startDate, LocalDate endDate);
}
