package edu.tcu.cs.projectpulse.war;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "war_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "week_start"})
})
public class WAREntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "section_name")
    private String sectionName;

    @OneToMany(mappedBy = "war", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WARActivityEntity> activities = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public List<WARActivityEntity> getActivities() { return activities; }
    public void setActivities(List<WARActivityEntity> activities) { this.activities = activities; }

    public void addActivity(WARActivityEntity activity) {
        activities.add(activity);
        activity.setWar(this);
    }

    public void removeActivity(WARActivityEntity activity) {
        activities.remove(activity);
        activity.setWar(null);
    }
}
