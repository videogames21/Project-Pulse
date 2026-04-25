package edu.tcu.cs.projectpulse.war;

import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamNotFoundException;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import edu.tcu.cs.projectpulse.war.dto.StudentWARRangeReportResponse;
import edu.tcu.cs.projectpulse.war.dto.StudentWARSummary;
import edu.tcu.cs.projectpulse.war.dto.TeamWARReportResponse;
import edu.tcu.cs.projectpulse.war.dto.WARActivityRequest;
import edu.tcu.cs.projectpulse.war.dto.WARActivityResponse;
import edu.tcu.cs.projectpulse.war.dto.WeeklyWARSummary;
import edu.tcu.cs.projectpulse.war.dto.WARResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class WARService {

    private final WARRepository warRepository;
    private final WARActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public WARService(WARRepository warRepository,
                      WARActivityRepository activityRepository,
                      UserRepository userRepository,
                      TeamRepository teamRepository) {
        this.warRepository = warRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    public WARResponse getWAR(Long studentId, LocalDate weekStart) {
        validateStudentExists(studentId);
        validateWeekStart(weekStart);
        WAREntity war = warRepository.findByStudentIdAndWeekStart(studentId, weekStart)
                .orElse(null);
        if (war == null) {
            WAREntity empty = new WAREntity();
            empty.setStudentId(studentId);
            empty.setWeekStart(weekStart);
            return toResponse(empty);
        }
        return toResponse(war);
    }

    @Transactional
    public WARActivityResponse addActivity(Long studentId, LocalDate weekStart, WARActivityRequest request) {
        validateStudentExists(studentId);
        validateWeekStart(weekStart);
        validateNotFutureWeek(weekStart);

        WAREntity war = warRepository.findByStudentIdAndWeekStart(studentId, weekStart)
                .orElseGet(() -> {
                    WAREntity newWar = new WAREntity();
                    newWar.setStudentId(studentId);
                    newWar.setWeekStart(weekStart);
                    return warRepository.save(newWar);
                });

        WARActivityEntity activity = new WARActivityEntity();
        activity.setCategory(request.category());
        activity.setDescription(request.description());
        activity.setPlannedHours(request.plannedHours());
        activity.setActualHours(request.actualHours());
        activity.setStatus(request.status());
        activity.setWar(war);
        WARActivityEntity saved = activityRepository.save(activity);
        war.getActivities().add(saved);

        return toActivityResponse(saved);
    }

    @Transactional
    public WARActivityResponse updateActivity(Long studentId, LocalDate weekStart, Long activityId,
                                              WARActivityRequest request) {
        validateStudentExists(studentId);
        validateWeekStart(weekStart);

        WAREntity war = warRepository.findByStudentIdAndWeekStart(studentId, weekStart)
                .orElseThrow(() -> new WARNotFoundException(studentId, weekStart));

        WARActivityEntity activity = war.getActivities().stream()
                .filter(a -> a.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new WARNotFoundException(activityId));

        activity.setCategory(request.category());
        activity.setDescription(request.description());
        activity.setPlannedHours(request.plannedHours());
        activity.setActualHours(request.actualHours());
        activity.setStatus(request.status());
        warRepository.save(war);

        return toActivityResponse(activity);
    }

    @Transactional
    public void deleteActivity(Long studentId, LocalDate weekStart, Long activityId) {
        validateStudentExists(studentId);
        validateWeekStart(weekStart);

        WAREntity war = warRepository.findByStudentIdAndWeekStart(studentId, weekStart)
                .orElseThrow(() -> new WARNotFoundException(studentId, weekStart));

        WARActivityEntity activity = war.getActivities().stream()
                .filter(a -> a.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new WARNotFoundException(activityId));

        war.removeActivity(activity);
        warRepository.save(war);
    }

    // ── UC-32: Team WAR report ───────────────────────────────────────────────

    public TeamWARReportResponse getTeamReport(Long teamId, LocalDate weekStart) {
        validateWeekStart(weekStart);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        List<StudentWARSummary> students = userRepository
                .findByRoleAndTeamId(UserRole.STUDENT, teamId)
                .stream()
                .sorted(Comparator.comparing(UserEntity::getLastName))
                .map(student -> {
                    WAREntity war = warRepository
                            .findByStudentIdAndWeekStart(student.getId(), weekStart)
                            .orElse(null);
                    boolean didSubmit = war != null;
                    List<WARActivityResponse> activities = didSubmit
                            ? war.getActivities().stream().map(this::toActivityResponse).toList()
                            : List.of();
                    String fullName = student.getFirstName() + " " + student.getLastName();
                    return new StudentWARSummary(student.getId(), fullName, didSubmit, activities);
                })
                .toList();

        return new TeamWARReportResponse(teamId, team.getName(), weekStart, students);
    }

    // ── UC-34: Student WAR range report ─────────────────────────────────────

    public StudentWARRangeReportResponse getStudentRangeReport(Long studentId,
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

        List<WAREntity> wars = warRepository
                .findAllByStudentIdAndWeekStartBetween(studentId, startWeek, endWeek)
                .stream()
                .sorted(Comparator.comparing(WAREntity::getWeekStart))
                .toList();

        List<WeeklyWARSummary> weeks = wars.stream()
                .map(war -> new WeeklyWARSummary(
                        war.getWeekStart(),
                        war.getActivities().stream().map(this::toActivityResponse).toList()))
                .toList();

        return new StudentWARRangeReportResponse(
                studentId, student.getFirstName() + " " + student.getLastName(),
                startWeek, endWeek, weeks);
    }

    private void validateStudentExists(Long studentId) {
        UserEntity user = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + studentId + " is not a student.");
        }
    }

    private void validateWeekStart(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Week start date must be a Monday.");
        }
    }

    private void validateNotFutureWeek(LocalDate weekStart) {
        if (weekStart.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot add activities to a future week.");
        }
    }

    public WARResponse toResponse(WAREntity war) {
        List<WARActivityResponse> activities = war.getActivities().stream()
                .map(this::toActivityResponse)
                .toList();
        return new WARResponse(war.getId(), war.getStudentId(), war.getWeekStart(), activities);
    }

    public WARActivityResponse toActivityResponse(WARActivityEntity activity) {
        return new WARActivityResponse(
                activity.getId(),
                activity.getCategory(),
                activity.getDescription(),
                activity.getPlannedHours(),
                activity.getActualHours(),
                activity.getStatus()
        );
    }
}
