package edu.tcu.cs.projectpulse.rubric;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rubrics")
public class RubricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean active = false;

    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CriterionEntity> criteria = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<CriterionEntity> getCriteria() { return criteria; }
    public void setCriteria(List<CriterionEntity> criteria) { this.criteria = criteria; }

    public void addCriterion(CriterionEntity criterion) {
        criteria.add(criterion);
        criterion.setRubric(this);
    }
}
