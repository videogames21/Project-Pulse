package edu.tcu.cs.projectpulse.activeweek;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "active_weeks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"section_id", "week_start_date"})
})
public class ActiveWeekEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
