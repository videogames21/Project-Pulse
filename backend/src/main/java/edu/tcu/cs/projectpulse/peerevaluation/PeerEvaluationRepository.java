package edu.tcu.cs.projectpulse.peerevaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeerEvaluationRepository extends JpaRepository<PeerEvaluationEntity, Long> {
    Optional<PeerEvaluationEntity> findFirstByEvaluatorIdAndWeek(Long evaluatorId, Integer week);
    boolean existsByEvaluatorIdAndWeek(Long evaluatorId, Integer week);
    List<PeerEvaluationEntity> findByEvaluateeIdAndWeek(Long evaluateeId, Integer week);
    List<PeerEvaluationEntity> findByEvaluateeIdAndWeekBetween(Long evaluateeId, Integer startWeek, Integer endWeek);
    List<PeerEvaluationEntity> findByEvaluateeIdInAndWeek(List<Long> evaluateeIds, Integer week);
}
