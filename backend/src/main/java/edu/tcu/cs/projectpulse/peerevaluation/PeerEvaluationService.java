package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.peerevaluation.dto.CriterionAverageScoreResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvaluationRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvaluationResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.ScoreRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.ScoreResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.StudentPeerEvaluationReportResponse;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PeerEvaluationService {

    private final PeerEvaluationRepository peerEvaluationRepository;
    private final UserRepository userRepository;

    public PeerEvaluationService(PeerEvaluationRepository peerEvaluationRepository,
                                  UserRepository userRepository) {
        this.peerEvaluationRepository = peerEvaluationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PeerEvaluationResponse submit(PeerEvaluationRequest request) {
        validateWeekStart(request.weekStart());
        validateNotFutureWeek(request.weekStart());

        UserEntity evaluator = requireStudent(request.evaluatorId());
        UserEntity evaluatee = requireStudent(request.evaluateeId());
        validateSameTeam(evaluator, evaluatee);

        if (peerEvaluationRepository.existsByEvaluatorIdAndEvaluateeIdAndWeekStart(
                request.evaluatorId(), request.evaluateeId(), request.weekStart())) {
            throw new IllegalStateException(
                    "A peer evaluation already exists for this evaluator, evaluatee, and week. Use PUT to update it.");
        }

        PeerEvaluationEntity entity = buildEntity(request);
        PeerEvaluationEntity saved = peerEvaluationRepository.save(entity);
        saveScores(saved, request.scores());
        return toResponse(saved);
    }

    @Transactional
    public PeerEvaluationResponse update(Long id, PeerEvaluationRequest request) {
        PeerEvaluationEntity existing = peerEvaluationRepository.findById(id)
                .orElseThrow(() -> new PeerEvaluationNotFoundException(id));

        validateWeekStart(request.weekStart());

        UserEntity evaluator = requireStudent(request.evaluatorId());
        UserEntity evaluatee = requireStudent(request.evaluateeId());
        validateSameTeam(evaluator, evaluatee);

        existing.setPublicComments(request.publicComments());
        existing.setPrivateComments(request.privateComments());

        existing.getScores().clear();
        peerEvaluationRepository.saveAndFlush(existing);

        saveScores(existing, request.scores());
        PeerEvaluationEntity saved = peerEvaluationRepository.save(existing);
        return toResponse(saved);
    }

    public PeerEvaluationResponse findById(Long id) {
        return toResponse(peerEvaluationRepository.findById(id)
                .orElseThrow(() -> new PeerEvaluationNotFoundException(id)));
    }

    // ── Report helpers (used by report endpoints) ────────────────────────────

    public List<PeerEvaluationEntity> findByEvaluateeAndWeek(Long evaluateeId, LocalDate weekStart) {
        return peerEvaluationRepository.findAllByEvaluateeIdAndWeekStart(evaluateeId, weekStart);
    }

    public List<PeerEvaluationEntity> findByEvaluateeAndRange(Long evaluateeId,
                                                               LocalDate startWeek, LocalDate endWeek) {
        return peerEvaluationRepository
                .findAllByEvaluateeIdAndWeekStartBetweenOrderByWeekStartAsc(evaluateeId, startWeek, endWeek);
    }

    public List<PeerEvaluationEntity> findByWeek(LocalDate weekStart) {
        return peerEvaluationRepository.findAllByWeekStart(weekStart);
    }

    // ── UC-29: Student report ────────────────────────────────────────────────

    public StudentPeerEvaluationReportResponse getStudentReport(Long studentId, LocalDate weekStart) {
        validateWeekStart(weekStart);

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));

        List<PeerEvaluationEntity> evaluations =
                peerEvaluationRepository.findAllByEvaluateeIdAndWeekStart(studentId, weekStart);

        if (evaluations.isEmpty()) {
            return new StudentPeerEvaluationReportResponse(
                    studentId, student.getName(), weekStart, 0, List.of(), List.of(), BigDecimal.ZERO);
        }

        // Per-criterion averages (sorted by criterionId for deterministic output)
        Map<Long, List<Integer>> scoresByCriterion = new LinkedHashMap<>();
        for (PeerEvaluationEntity eval : evaluations) {
            for (PeerEvaluationScoreEntity score : eval.getScores()) {
                scoresByCriterion
                        .computeIfAbsent(score.getCriterionId(), k -> new ArrayList<>())
                        .add(score.getScore());
            }
        }

        List<CriterionAverageScoreResponse> averageCriterionScores = scoresByCriterion.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal sum = entry.getValue().stream()
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = sum.divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
                    return new CriterionAverageScoreResponse(entry.getKey(), avg);
                })
                .toList();

        // Average grade = average of per-evaluation totals
        BigDecimal totalSum = evaluations.stream()
                .map(eval -> eval.getScores().stream()
                        .map(s -> BigDecimal.valueOf(s.getScore()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageGrade = totalSum.divide(BigDecimal.valueOf(evaluations.size()), 2, RoundingMode.HALF_UP);

        // Public comments — filter blank/null; private comments excluded (BR-5)
        List<String> publicComments = evaluations.stream()
                .map(PeerEvaluationEntity::getPublicComments)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        return new StudentPeerEvaluationReportResponse(
                studentId, student.getName(), weekStart,
                evaluations.size(), averageCriterionScores, publicComments, averageGrade);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private PeerEvaluationEntity buildEntity(PeerEvaluationRequest request) {
        PeerEvaluationEntity entity = new PeerEvaluationEntity();
        entity.setEvaluatorId(request.evaluatorId());
        entity.setEvaluateeId(request.evaluateeId());
        entity.setWeekStart(request.weekStart());
        entity.setPublicComments(request.publicComments());
        entity.setPrivateComments(request.privateComments());
        return entity;
    }

    private void saveScores(PeerEvaluationEntity entity, List<ScoreRequest> scoreRequests) {
        for (ScoreRequest sr : scoreRequests) {
            PeerEvaluationScoreEntity score = new PeerEvaluationScoreEntity();
            score.setPeerEvaluation(entity);
            score.setCriterionId(sr.criterionId());
            score.setScore(sr.score());
            entity.getScores().add(score);
        }
        peerEvaluationRepository.save(entity);
    }

    private UserEntity requireStudent(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + userId + " is not a student.");
        }
        return user;
    }

    private void validateSameTeam(UserEntity evaluator, UserEntity evaluatee) {
        if (evaluator.getTeamId() == null) {
            throw new IllegalStateException("Evaluator (id=" + evaluator.getId() + ") is not assigned to any team.");
        }
        if (evaluatee.getTeamId() == null) {
            throw new IllegalStateException("Evaluatee (id=" + evaluatee.getId() + ") is not assigned to any team.");
        }
        if (!evaluator.getTeamId().equals(evaluatee.getTeamId())) {
            throw new IllegalStateException("Evaluator and evaluatee must be on the same team.");
        }
    }

    private void validateWeekStart(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Week start date must be a Monday.");
        }
    }

    private void validateNotFutureWeek(LocalDate weekStart) {
        if (weekStart.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot submit a peer evaluation for a future week.");
        }
    }

    public PeerEvaluationResponse toResponse(PeerEvaluationEntity entity) {
        List<ScoreResponse> scores = entity.getScores().stream()
                .map(s -> new ScoreResponse(s.getCriterionId(), s.getScore()))
                .toList();
        return new PeerEvaluationResponse(
                entity.getId(),
                entity.getEvaluatorId(),
                entity.getEvaluateeId(),
                entity.getWeekStart(),
                scores,
                entity.getPublicComments(),
                entity.getPrivateComments()
        );
    }
}
