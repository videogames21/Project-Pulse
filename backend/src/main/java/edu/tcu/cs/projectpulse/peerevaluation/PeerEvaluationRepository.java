package edu.tcu.cs.projectpulse.peerevaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeerEvaluationRepository extends JpaRepository<PeerEvaluationEntity, Long> {

    Optional<PeerEvaluationEntity> findByEvaluatorIdAndEvaluateeIdAndWeekStart(
            Long evaluatorId, Long evaluateeId, LocalDate weekStart);

    List<PeerEvaluationEntity> findAllByEvaluateeIdAndWeekStart(Long evaluateeId, LocalDate weekStart);

    List<PeerEvaluationEntity> findAllByEvaluateeIdAndWeekStartBetween(
            Long evaluateeId, LocalDate startWeek, LocalDate endWeek);

    List<PeerEvaluationEntity> findAllByEvaluateeIdAndWeekStartBetweenOrderByWeekStartAsc(
            Long evaluateeId, LocalDate startWeek, LocalDate endWeek);

    List<PeerEvaluationEntity> findAllByWeekStart(LocalDate weekStart);

    boolean existsByEvaluatorIdAndEvaluateeIdAndWeekStart(
            Long evaluatorId, Long evaluateeId, LocalDate weekStart);

    boolean existsByEvaluatorIdAndWeekStart(Long evaluatorId, LocalDate weekStart);

    List<PeerEvaluationEntity> findAllByEvaluateeId(Long evaluateeId);

    List<PeerEvaluationEntity> findAllByEvaluatorId(Long evaluatorId);

    List<PeerEvaluationEntity> findAllByEvaluatorIdAndWeekStart(Long evaluatorId, LocalDate weekStart);
}
