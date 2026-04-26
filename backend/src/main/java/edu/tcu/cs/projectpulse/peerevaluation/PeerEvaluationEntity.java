package edu.tcu.cs.projectpulse.peerevaluation;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "peer_evaluations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"evaluator_id", "evaluatee_id", "week_start"})
})
public class PeerEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluator_id", nullable = false)
    private Long evaluatorId;

    @Column(name = "evaluatee_id", nullable = false)
    private Long evaluateeId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "public_comments", columnDefinition = "TEXT")
    private String publicComments;

    @Column(name = "private_comments", columnDefinition = "TEXT")
    private String privateComments;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "section_name")
    private String sectionName;

    @OneToMany(mappedBy = "peerEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeerEvaluationScoreEntity> scores = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }

    public Long getEvaluateeId() { return evaluateeId; }
    public void setEvaluateeId(Long evaluateeId) { this.evaluateeId = evaluateeId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public String getPublicComments() { return publicComments; }
    public void setPublicComments(String publicComments) { this.publicComments = publicComments; }

    public String getPrivateComments() { return privateComments; }
    public void setPrivateComments(String privateComments) { this.privateComments = privateComments; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public List<PeerEvaluationScoreEntity> getScores() { return scores; }
    public void setScores(List<PeerEvaluationScoreEntity> scores) { this.scores = scores; }
}
