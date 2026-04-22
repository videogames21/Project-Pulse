package edu.tcu.cs.projectpulse.rubric;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "criteria")
public class CriterionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

    @ManyToOne
    @JoinColumn(name = "rubric_id", nullable = false)
    private RubricEntity rubric;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public RubricEntity getRubric() { return rubric; }
    public void setRubric(RubricEntity rubric) { this.rubric = rubric; }
}
