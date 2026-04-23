package edu.tcu.cs.projectpulse.report;

import edu.tcu.cs.projectpulse.peerevaluation.CriterionScoreEntity;
import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationEntity;
import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationRepository;
import edu.tcu.cs.projectpulse.report.dto.*;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.war.WAREntryEntity;
import edu.tcu.cs.projectpulse.war.WAREntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final PeerEvaluationRepository peerEvalRepo;
    private final WAREntryRepository warEntryRepo;
    private final UserRepository userRepo;
    private final TeamRepository teamRepo;

    public ReportService(PeerEvaluationRepository peerEvalRepo,
                         WAREntryRepository warEntryRepo,
                         UserRepository userRepo,
                         TeamRepository teamRepo) {
        this.peerEvalRepo = peerEvalRepo;
        this.warEntryRepo = warEntryRepo;
        this.userRepo = userRepo;
        this.teamRepo = teamRepo;
    }

    // UC-29: Student views their own peer eval report for a week
    public StudentPeerEvalReportResponse getMyPeerEvalReport(Long studentId, Integer week) {
        List<PeerEvaluationEntity> evals = peerEvalRepo.findByEvaluateeIdAndWeek(studentId, week);

        List<CriterionAvgResponse> criteriaAvgs = buildCriterionAverages(evals);
        double overallScore = criteriaAvgs.stream().mapToDouble(CriterionAvgResponse::avgScore).sum();
        double maxScore = criteriaAvgs.stream()
                .mapToDouble(c -> c.maxScore().doubleValue()).sum();

        List<String> publicComments = evals.stream()
                .map(PeerEvaluationEntity::getPublicComment)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        return new StudentPeerEvalReportResponse(week, overallScore, maxScore, criteriaAvgs, publicComments);
    }

    // UC-31: Instructor views peer eval status for all students in the section
    public List<SectionPeerEvalRow> getSectionPeerEvalReport(Integer week) {
        List<UserEntity> students = userRepo.findByRole("STUDENT");

        return students.stream()
                .sorted(Comparator.comparing(UserEntity::getLastName))
                .map(student -> {
            List<PeerEvaluationEntity> received = peerEvalRepo.findByEvaluateeIdAndWeek(student.getId(), week);
            boolean submitted = peerEvalRepo.existsByEvaluatorIdAndWeek(student.getId(), week);

            double totalScore = received.stream()
                    .flatMap(e -> e.getCriterionScores().stream())
                    .mapToInt(CriterionScoreEntity::getScore).sum();

            double maxScore = received.isEmpty() ? 0 : received.get(0).getCriterionScores().stream()
                    .mapToDouble(cs -> cs.getCriterion().getMaxScore().doubleValue()).sum();

            int teamSize = teamRepo.findFirstByMembersId(student.getId())
                    .map(t -> t.getMembers().size()).orElse(1);

            return new SectionPeerEvalRow(
                    student.getId(), student.getFirstName(), student.getLastName(),
                    submitted, totalScore, maxScore, received.size(), teamSize
            );
        }).toList();
    }

    // UC-32: Instructor views WAR summary for a team in a week
    public List<TeamWARRow> getTeamWARReport(Long teamId, Integer week) {
        TeamEntity team = teamRepo.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id " + teamId));

        return team.getMembers().stream()
                .sorted(Comparator.comparing(UserEntity::getLastName))
                .map(member -> {
            List<WAREntryEntity> entries = warEntryRepo
                    .findByStudentIdAndWeekOrderByIdAsc(member.getId(), week);

            BigDecimal planned = entries.stream()
                    .map(WAREntryEntity::getPlannedHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal actual = entries.stream()
                    .map(WAREntryEntity::getActualHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new TeamWARRow(
                    member.getId(), member.getFirstName(), member.getLastName(),
                    entries.size(), planned, actual
            );
        }).toList();
    }

    // UC-33: Instructor views full peer eval detail for a student across weeks
    public StudentPeerEvalDetailReport getStudentPeerEvalDetail(Long studentId, Integer startWeek, Integer endWeek) {
        UserEntity student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + studentId));

        List<PeerEvaluationEntity> allEvals =
                peerEvalRepo.findByEvaluateeIdAndWeekBetween(studentId, startWeek, endWeek);

        Map<Integer, List<PeerEvaluationEntity>> byWeek = allEvals.stream()
                .collect(Collectors.groupingBy(PeerEvaluationEntity::getWeek));

        List<WeekPeerEvalDetail> weeks = new ArrayList<>();
        for (int w = startWeek; w <= endWeek; w++) {
            List<PeerEvaluationEntity> evals = byWeek.getOrDefault(w, List.of());

            List<CriterionAvgResponse> criteriaAvgs = buildCriterionAverages(evals);
            double overallScore = criteriaAvgs.stream().mapToDouble(CriterionAvgResponse::avgScore).sum();
            double maxScore = criteriaAvgs.stream().mapToDouble(c -> c.maxScore().doubleValue()).sum();

            List<EvaluationDetail> evalDetails = evals.stream().map(e -> {
                String evaluatorName = e.getEvaluator().getFirstName() + " " + e.getEvaluator().getLastName();
                int total = e.getCriterionScores().stream().mapToInt(CriterionScoreEntity::getScore).sum();
                List<CriterionScoreDetail> scores = e.getCriterionScores().stream()
                        .map(cs -> new CriterionScoreDetail(
                                cs.getCriterion().getId(),
                                cs.getCriterion().getName(),
                                cs.getScore(),
                                cs.getCriterion().getMaxScore()))
                        .toList();
                return new EvaluationDetail(
                        e.getEvaluator().getId(), evaluatorName, total,
                        scores, e.getPublicComment(), e.getPrivateComment());
            }).toList();

            weeks.add(new WeekPeerEvalDetail(w, overallScore, maxScore, criteriaAvgs, evalDetails));
        }

        return new StudentPeerEvalDetailReport(
                student.getId(), student.getFirstName(), student.getLastName(), weeks);
    }

    // UC-34: Instructor views WAR detail for a student across weeks
    public StudentWARDetailReport getStudentWARDetail(Long studentId, Integer startWeek, Integer endWeek) {
        UserEntity student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + studentId));

        List<WAREntryEntity> allEntries =
                warEntryRepo.findByStudentIdAndWeekBetweenOrderByWeekAscIdAsc(studentId, startWeek, endWeek);

        Map<Integer, List<WAREntryEntity>> byWeek = allEntries.stream()
                .collect(Collectors.groupingBy(WAREntryEntity::getWeek));

        List<WeekWARDetail> weeks = new ArrayList<>();
        for (int w = startWeek; w <= endWeek; w++) {
            List<WAREntryEntity> entries = byWeek.getOrDefault(w, List.of());

            List<ActivityDetail> activities = entries.stream()
                    .map(e -> new ActivityDetail(
                            e.getId(), e.getCategory(), e.getDescription(),
                            e.getPlannedHours(), e.getActualHours(), e.getStatus()))
                    .toList();

            BigDecimal planned = entries.stream().map(WAREntryEntity::getPlannedHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal actual = entries.stream().map(WAREntryEntity::getActualHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            weeks.add(new WeekWARDetail(w, activities, planned, actual));
        }

        return new StudentWARDetailReport(
                student.getId(), student.getFirstName(), student.getLastName(), weeks);
    }

    // Shared helper: compute per-criterion averages across a list of evaluations
    private List<CriterionAvgResponse> buildCriterionAverages(List<PeerEvaluationEntity> evals) {
        if (evals.isEmpty()) return List.of();

        Map<Long, List<Integer>> scoresByCriterion = new LinkedHashMap<>();
        Map<Long, CriterionScoreEntity> metaByCriterion = new LinkedHashMap<>();

        for (PeerEvaluationEntity e : evals) {
            for (CriterionScoreEntity cs : e.getCriterionScores()) {
                Long cid = cs.getCriterion().getId();
                scoresByCriterion.computeIfAbsent(cid, k -> new ArrayList<>()).add(cs.getScore());
                metaByCriterion.putIfAbsent(cid, cs);
            }
        }

        return scoresByCriterion.entrySet().stream().map(entry -> {
            Long cid = entry.getKey();
            List<Integer> scores = entry.getValue();
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            CriterionScoreEntity meta = metaByCriterion.get(cid);
            return new CriterionAvgResponse(
                    cid,
                    meta.getCriterion().getName(),
                    meta.getCriterion().getDescription(),
                    Math.round(avg * 10.0) / 10.0,
                    meta.getCriterion().getMaxScore()
            );
        }).toList();
    }
}
