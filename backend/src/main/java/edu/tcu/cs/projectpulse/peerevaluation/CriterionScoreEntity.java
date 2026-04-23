package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.rubric.CriterionEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "criterion_scores")
public class CriterionScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private PeerEvaluationEntity evaluation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private CriterionEntity criterion;

    @Column(nullable = false)
    private Integer score;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PeerEvaluationEntity getEvaluation() { return evaluation; }
    public void setEvaluation(PeerEvaluationEntity evaluation) { this.evaluation = evaluation; }

    public CriterionEntity getCriterion() { return criterion; }
    public void setCriterion(CriterionEntity criterion) { this.criterion = criterion; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
