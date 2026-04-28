package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.peerevaluation.dto.CriterionAverageScoreResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvaluationRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvaluationResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.ReceivedEvaluationDetail;
import edu.tcu.cs.projectpulse.peerevaluation.dto.ScoreRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.ScoreResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.SectionPeerEvaluationReportResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.StudentEvaluationSummary;
import edu.tcu.cs.projectpulse.peerevaluation.dto.StudentPeerEvalRangeReportResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.StudentPeerEvaluationReportResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.WeeklyPeerEvalSummary;
import edu.tcu.cs.projectpulse.rubric.CriterionEntity;
import edu.tcu.cs.projectpulse.rubric.CriterionRepository;
import edu.tcu.cs.projectpulse.rubric.RubricRepository;
import edu.tcu.cs.projectpulse.section.SectionNotFoundException;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.team.TeamRepository;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PeerEvaluationService {

    private final PeerEvaluationRepository peerEvaluationRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;
    private final CriterionRepository criterionRepository;
    private final RubricRepository rubricRepository;

    public PeerEvaluationService(PeerEvaluationRepository peerEvaluationRepository,
                                  UserRepository userRepository,
                                  TeamRepository teamRepository,
                                  SectionRepository sectionRepository,
                                  CriterionRepository criterionRepository,
                                  RubricRepository rubricRepository) {
        this.peerEvaluationRepository = peerEvaluationRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.sectionRepository = sectionRepository;
        this.criterionRepository = criterionRepository;
        this.rubricRepository = rubricRepository;
    }

    @Transactional
    public PeerEvaluationResponse submit(PeerEvaluationRequest request) {
        validateWeekStart(request.weekStart());
        validateIsPreviousWeek(request.weekStart());

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

    @Transactional(readOnly = true)
    public PeerEvaluationResponse findById(Long id) {
        return toResponse(peerEvaluationRepository.findById(id)
                .orElseThrow(() -> new PeerEvaluationNotFoundException(id)));
    }

    // ── UC-28: Fetch evaluations already submitted by an evaluator for a week ──

    @Transactional(readOnly = true)
    public List<PeerEvaluationResponse> findByEvaluatorAndWeek(Long evaluatorId, LocalDate weekStart) {
        validateWeekStart(weekStart);
        return peerEvaluationRepository.findAllByEvaluatorIdAndWeekStart(evaluatorId, weekStart)
                .stream()
                .map(this::toResponse)
                .toList();
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

    @Transactional(readOnly = true)
    public StudentPeerEvaluationReportResponse getStudentReport(Long studentId, LocalDate weekStart) {
        validateWeekStart(weekStart);

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + studentId + " is not a student.");
        }

        List<PeerEvaluationEntity> evaluations =
                peerEvaluationRepository.findAllByEvaluateeIdAndWeekStart(studentId, weekStart);

        if (evaluations.isEmpty()) {
            return new StudentPeerEvaluationReportResponse(
                    studentId, student.getFirstName() + " " + student.getLastName(), weekStart, 0, List.of(), List.of(), BigDecimal.ZERO);
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
                    String name = criterionRepository.findById(entry.getKey())
                            .map(c -> c.getName())
                            .orElse("Criterion " + entry.getKey());
                    return new CriterionAverageScoreResponse(entry.getKey(), name, avg);
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
                studentId, student.getFirstName() + " " + student.getLastName(), weekStart,
                evaluations.size(), averageCriterionScores, publicComments, averageGrade);
    }

    // ── UC-31: Section report ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SectionPeerEvaluationReportResponse getSectionReport(String sectionName, LocalDate weekStart) {
        validateWeekStart(weekStart);

        if (!sectionRepository.existsByName(sectionName)) {
            throw new SectionNotFoundException(sectionName);
        }

        // Compute max possible grade from the section's rubric (sum of all criterion maxScores)
        BigDecimal maxGrade = sectionRepository.findByName(sectionName)
                .filter(s -> s.getRubricId() != null)
                .flatMap(s -> rubricRepository.findById(s.getRubricId()))
                .map(rubric -> rubric.getCriteria().stream()
                        .map(CriterionEntity::getMaxScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);

        // Collect all students in the section, sorted by last name
        List<UserEntity> students = teamRepository.findAllBySectionNameOrderByNameAsc(sectionName)
                .stream()
                .flatMap(team -> userRepository.findByRoleAndTeamId(UserRole.STUDENT, team.getId()).stream())
                .sorted(Comparator.comparing(UserEntity::getLastName))
                .toList();

        List<StudentEvaluationSummary> summaries = students.stream()
                .map(student -> buildStudentSummary(student, weekStart))
                .toList();

        return new SectionPeerEvaluationReportResponse(sectionName, weekStart, maxGrade, summaries);
    }

    private StudentEvaluationSummary buildStudentSummary(UserEntity student, LocalDate weekStart) {
        boolean didSubmit = peerEvaluationRepository
                .existsByEvaluatorIdAndWeekStart(student.getId(), weekStart);

        List<PeerEvaluationEntity> received =
                peerEvaluationRepository.findAllByEvaluateeIdAndWeekStart(student.getId(), weekStart);

        List<ReceivedEvaluationDetail> details = received.stream()
                .map(eval -> {
                    String evaluatorName = userRepository.findById(eval.getEvaluatorId())
                            .map(u -> u.getFirstName() + " " + u.getLastName())
                            .orElse("Unknown");
                    BigDecimal total = eval.getScores().stream()
                            .map(s -> BigDecimal.valueOf(s.getScore()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ReceivedEvaluationDetail(
                            eval.getEvaluatorId(), evaluatorName, total,
                            eval.getPublicComments(), eval.getPrivateComments());
                })
                .toList();

        BigDecimal averageGrade = received.isEmpty() ? BigDecimal.ZERO :
                details.stream()
                        .map(ReceivedEvaluationDetail::totalScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(received.size()), 2, RoundingMode.HALF_UP);

        return new StudentEvaluationSummary(
                student.getId(), student.getFirstName() + " " + student.getLastName(), didSubmit,
                received.size(), averageGrade, details);
    }


    // ── UC-33: Instructor range report for a student ─────────────────────────

    @Transactional(readOnly = true)
    public StudentPeerEvalRangeReportResponse getStudentRangeReport(Long studentId,
                                                                     LocalDate startWeek,
                                                                     LocalDate endWeek) {
        validateWeekStart(startWeek);
        validateWeekStart(endWeek);
        if (endWeek.isBefore(startWeek)) {
            throw new IllegalArgumentException("endWeek must not be before startWeek.");
        }

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + studentId + " is not a student.");
        }

        List<PeerEvaluationEntity> all =
                peerEvaluationRepository.findAllByEvaluateeIdAndWeekStartBetweenOrderByWeekStartAsc(
                        studentId, startWeek, endWeek);

        // Group by week (preserving chronological order)
        Map<LocalDate, List<PeerEvaluationEntity>> byWeek = new LinkedHashMap<>();
        for (PeerEvaluationEntity eval : all) {
            byWeek.computeIfAbsent(eval.getWeekStart(), k -> new ArrayList<>()).add(eval);
        }

        List<WeeklyPeerEvalSummary> weeks = byWeek.entrySet().stream()
                .map(entry -> {
                    List<PeerEvaluationEntity> evals = entry.getValue();
                    List<ReceivedEvaluationDetail> details = evals.stream()
                            .map(eval -> {
                                String evaluatorName = userRepository.findById(eval.getEvaluatorId())
                                        .map(u -> u.getFirstName() + " " + u.getLastName())
                                        .orElse("Unknown");
                                BigDecimal total = eval.getScores().stream()
                                        .map(s -> BigDecimal.valueOf(s.getScore()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new ReceivedEvaluationDetail(
                                        eval.getEvaluatorId(), evaluatorName, total,
                                        eval.getPublicComments(), eval.getPrivateComments());
                            })
                            .toList();
                    BigDecimal avgGrade = details.stream()
                            .map(ReceivedEvaluationDetail::totalScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);
                    return new WeeklyPeerEvalSummary(entry.getKey(), evals.size(), avgGrade, details);
                })
                .toList();

        return new StudentPeerEvalRangeReportResponse(
                studentId, student.getFirstName() + " " + student.getLastName(),
                startWeek, endWeek, weeks);
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

    private void validateIsPreviousWeek(LocalDate weekStart) {
        LocalDate previousMonday = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .minusWeeks(1);
        if (!weekStart.equals(previousMonday)) {
            throw new IllegalArgumentException(
                    "Peer evaluations can only be submitted for the previous week (week starting "
                    + previousMonday + ").");
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
