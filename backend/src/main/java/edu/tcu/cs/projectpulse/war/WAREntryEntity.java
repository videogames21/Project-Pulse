package edu.tcu.cs.projectpulse.war;

import edu.tcu.cs.projectpulse.user.UserEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "war_entries")
public class WAREntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(nullable = false)
    private Integer week;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "planned_hours", nullable = false, precision = 5, scale = 1)
    private BigDecimal plannedHours;

    @Column(name = "actual_hours", nullable = false, precision = 5, scale = 1)
    private BigDecimal actualHours;

    @Column(nullable = false)
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getStudent() { return student; }
    public void setStudent(UserEntity student) { this.student = student; }

    public Integer getWeek() { return week; }
    public void setWeek(Integer week) { this.week = week; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPlannedHours() { return plannedHours; }
    public void setPlannedHours(BigDecimal plannedHours) { this.plannedHours = plannedHours; }

    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal actualHours) { this.actualHours = actualHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
