package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.user.UserEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "peer_evaluations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evaluator_id", "evaluatee_id", "week"}))
public class PeerEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private UserEntity evaluator;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluatee_id", nullable = false)
    private UserEntity evaluatee;

    @Column(nullable = false)
    private Integer week;

    @Column(name = "public_comment", length = 2000)
    private String publicComment;

    @Column(name = "private_comment", length = 2000)
    private String privateComment;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CriterionScoreEntity> criterionScores = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getEvaluator() { return evaluator; }
    public void setEvaluator(UserEntity evaluator) { this.evaluator = evaluator; }

    public UserEntity getEvaluatee() { return evaluatee; }
    public void setEvaluatee(UserEntity evaluatee) { this.evaluatee = evaluatee; }

    public Integer getWeek() { return week; }
    public void setWeek(Integer week) { this.week = week; }

    public String getPublicComment() { return publicComment; }
    public void setPublicComment(String publicComment) { this.publicComment = publicComment; }

    public String getPrivateComment() { return privateComment; }
    public void setPrivateComment(String privateComment) { this.privateComment = privateComment; }

    public List<CriterionScoreEntity> getCriterionScores() { return criterionScores; }
    public void setCriterionScores(List<CriterionScoreEntity> criterionScores) { this.criterionScores = criterionScores; }

    public void addCriterionScore(CriterionScoreEntity score) {
        criterionScores.add(score);
        score.setEvaluation(this);
    }
}
