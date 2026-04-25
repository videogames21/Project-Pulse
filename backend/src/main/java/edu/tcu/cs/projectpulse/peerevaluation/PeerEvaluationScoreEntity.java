package edu.tcu.cs.projectpulse.peerevaluation;

import jakarta.persistence.*;

@Entity
@Table(name = "peer_evaluation_scores")
public class PeerEvaluationScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peer_evaluation_id", nullable = false)
    private PeerEvaluationEntity peerEvaluation;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    @Column(nullable = false)
    private Integer score;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PeerEvaluationEntity getPeerEvaluation() { return peerEvaluation; }
    public void setPeerEvaluation(PeerEvaluationEntity peerEvaluation) { this.peerEvaluation = peerEvaluation; }

    public Long getCriterionId() { return criterionId; }
    public void setCriterionId(Long criterionId) { this.criterionId = criterionId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
