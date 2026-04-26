package edu.tcu.cs.projectpulse.student;

import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationEntity;
import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationRepository;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.student.dto.*;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import edu.tcu.cs.projectpulse.war.WAREntity;
import edu.tcu.cs.projectpulse.war.WARRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;
    private final WARRepository warRepository;
    private final PeerEvaluationRepository peerEvaluationRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(UserRepository userRepository,
                          TeamRepository teamRepository,
                          SectionRepository sectionRepository,
                          WARRepository warRepository,
                          PeerEvaluationRepository peerEvaluationRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.sectionRepository = sectionRepository;
        this.warRepository = warRepository;
        this.peerEvaluationRepository = peerEvaluationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StudentSummaryResponse> findStudents(
            String firstName, String lastName, String email,
            String sectionName, Long sectionId, String teamName, Long teamId,
            String sortBy, String sortDir,
            Authentication authentication) {

        UserEntity caller = loadCaller(authentication);
        boolean isAdmin = caller.getRole() == UserRole.ADMIN;

        // Resolve the set of team IDs the caller is allowed to see
        Set<Long> allowedTeamIds = null;

        if (!isAdmin) {
            // Instructor: force-filter to their own section only
            Optional<SectionEntity> section = sectionRepository.findByInstructorId(caller.getId());
            if (section.isEmpty()) return List.of();
            allowedTeamIds = teamRepository
                    .findAllBySectionNameOrderByNameAsc(section.get().getName())
                    .stream().map(TeamEntity::getId).collect(Collectors.toSet());
        } else {
            // Admin: apply optional section filter
            if (sectionId != null) {
                Optional<SectionEntity> sec = sectionRepository.findById(sectionId);
                if (sec.isEmpty()) return List.of();
                sectionName = sec.get().getName();
            }
            if (sectionName != null && !sectionName.isBlank()) {
                String sn = sectionName;
                allowedTeamIds = teamRepository
                        .findAllBySectionNameOrderByNameAsc(sn)
                        .stream().map(TeamEntity::getId).collect(Collectors.toSet());
            }
        }

        // Apply teamName filter (narrows allowedTeamIds further or sets it)
        if (teamName != null && !teamName.isBlank()) {
            String tn = teamName.toLowerCase();
            List<TeamEntity> matchingTeams = teamRepository.findAll().stream()
                    .filter(t -> t.getName().toLowerCase().contains(tn))
                    .toList();
            Set<Long> matchingTeamIds = matchingTeams.stream()
                    .map(TeamEntity::getId).collect(Collectors.toSet());
            if (allowedTeamIds != null) {
                matchingTeamIds.retainAll(allowedTeamIds);
            }
            allowedTeamIds = matchingTeamIds;
        }

        // Short-circuit: no teams in scope → no students
        if (allowedTeamIds != null && allowedTeamIds.isEmpty()) return List.of();

        // Build JPA specification
        Specification<UserEntity> spec = Specification.where(StudentSpec.isStudent())
                .and(StudentSpec.hasFirstName(firstName))
                .and(StudentSpec.hasLastName(lastName))
                .and(StudentSpec.hasEmail(email));

        if (teamId != null) {
            if (allowedTeamIds != null && !allowedTeamIds.contains(teamId)) {
                return List.of();
            }
            spec = spec.and(StudentSpec.hasTeamId(teamId));
        } else if (allowedTeamIds != null) {
            spec = spec.and(StudentSpec.teamIdIn(allowedTeamIds));
        }

        List<UserEntity> students = userRepository.findAll(spec);

        // Enrich with team and section info
        List<StudentSummaryResponse> enriched = students.stream()
                .map(this::toSummary)
                .collect(Collectors.toCollection(ArrayList::new));

        // Sort
        enriched.sort(buildComparator(sortBy, sortDir, isAdmin));
        return enriched;
    }

    public StudentDetailResponse viewStudent(Long id, Authentication authentication) {
        UserEntity caller = loadCaller(authentication);
        UserEntity student = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + id + " is not a student.");
        }

        if (caller.getRole() == UserRole.INSTRUCTOR) {
            enforceInstructorAccess(caller, student);
        }

        // Enrich header
        String teamName = null;
        String sectionName = null;
        Long sectionId = null;
        if (student.getTeamId() != null) {
            Optional<TeamEntity> team = teamRepository.findById(student.getTeamId());
            if (team.isPresent()) {
                teamName = team.get().getName();
                sectionName = team.get().getSectionName();
                sectionId = sectionRepository.findByName(sectionName)
                        .map(SectionEntity::getId).orElse(null);
            }
        }

        // WARs
        List<StudentWAREntry> wars = warRepository.findAllByStudentId(id).stream()
                .sorted(Comparator.comparing(WAREntity::getWeekStart))
                .map(war -> {
                    String wTeamName = war.getTeamId() != null
                            ? teamRepository.findById(war.getTeamId())
                                    .map(TeamEntity::getName).orElse("Unknown")
                            : "Unknown";
                    String wSectionName = war.getSectionName() != null ? war.getSectionName() : "Unknown";
                    return new StudentWAREntry(war.getId(), war.getWeekStart(),
                            war.getActivities().size(), wTeamName, wSectionName);
                })
                .toList();

        // Peer evals received
        List<StudentPeerEvalEntry> evals = peerEvaluationRepository.findAllByEvaluateeId(id).stream()
                .sorted(Comparator.comparing(PeerEvaluationEntity::getWeekStart))
                .map(eval -> {
                    String evaluatorName = userRepository.findById(eval.getEvaluatorId())
                            .map(UserEntity::getName).orElse("Unknown");
                    int totalScore = eval.getScores().stream()
                            .mapToInt(s -> s.getScore() != null ? s.getScore() : 0).sum();
                    String eTeamName = eval.getTeamId() != null
                            ? teamRepository.findById(eval.getTeamId())
                                    .map(TeamEntity::getName).orElse("Unknown")
                            : "Unknown";
                    String eSectionName = eval.getSectionName() != null ? eval.getSectionName() : "Unknown";
                    return new StudentPeerEvalEntry(eval.getId(), eval.getWeekStart(),
                            eval.getEvaluatorId(), evaluatorName, totalScore, eTeamName, eSectionName);
                })
                .toList();

        return new StudentDetailResponse(student.getId(), student.getFirstName(), student.getLastName(),
                student.getEmail(), student.getTeamId(), teamName, sectionId, sectionName, wars, evals);
    }

    @Transactional
    public void deleteStudent(Long id, String adminPassword, Authentication authentication) {
        UserEntity admin = loadCaller(authentication);
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            throw new BadCredentialsException("Incorrect admin password.");
        }

        UserEntity student = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + id + " is not a student.");
        }

        // Delete peer evaluations (evaluator or evaluatee)
        peerEvaluationRepository.deleteAllByEvaluatorIdOrEvaluateeId(id);

        // Delete WARs per-entity so CascadeType.ALL fires for war_activities
        warRepository.findAllByStudentId(id).forEach(warRepository::delete);

        userRepository.delete(student);
    }

    // --- helpers ---

    private UserEntity loadCaller(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(authentication.getName()));
    }

    private void enforceInstructorAccess(UserEntity instructor, UserEntity student) {
        Optional<SectionEntity> section = sectionRepository.findByInstructorId(instructor.getId());
        if (section.isEmpty()) {
            throw new AccessDeniedException("You are not authorized to view this student.");
        }
        Set<Long> visibleTeamIds = teamRepository
                .findAllBySectionNameOrderByNameAsc(section.get().getName())
                .stream().map(TeamEntity::getId).collect(Collectors.toSet());
        if (!visibleTeamIds.contains(student.getTeamId())) {
            throw new AccessDeniedException("You are not authorized to view this student.");
        }
    }

    private StudentSummaryResponse toSummary(UserEntity student) {
        String teamName = null;
        String sectionName = null;
        Long sectionId = null;
        if (student.getTeamId() != null) {
            Optional<TeamEntity> team = teamRepository.findById(student.getTeamId());
            if (team.isPresent()) {
                teamName = team.get().getName();
                sectionName = team.get().getSectionName();
                sectionId = sectionRepository.findByName(sectionName)
                        .map(SectionEntity::getId).orElse(null);
            }
        }
        return new StudentSummaryResponse(student.getId(), student.getFirstName(),
                student.getLastName(), student.getEmail(),
                student.getTeamId(), teamName, sectionId, sectionName);
    }

    private Comparator<StudentSummaryResponse> buildComparator(String sortBy, String sortDir, boolean isAdmin) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultComparator();
        }

        boolean desc = "desc".equalsIgnoreCase(sortDir);

        Comparator<StudentSummaryResponse> base = switch (sortBy.toLowerCase()) {
            case "firstname"   -> (a, b) -> nullSafe(a.firstName()).compareToIgnoreCase(nullSafe(b.firstName()));
            case "lastname"    -> (a, b) -> nullSafe(a.lastName()).compareToIgnoreCase(nullSafe(b.lastName()));
            case "email"       -> (a, b) -> nullSafe(a.email()).compareToIgnoreCase(nullSafe(b.email()));
            case "teamname"    -> (a, b) -> nullSafe(a.teamName()).compareToIgnoreCase(nullSafe(b.teamName()));
            case "teamid"      -> (Comparator<StudentSummaryResponse>) (a, b) -> Long.compare(
                    a.teamId() != null ? a.teamId() : Long.MAX_VALUE,
                    b.teamId() != null ? b.teamId() : Long.MAX_VALUE);
            case "sectionname" -> isAdmin
                    ? (Comparator<StudentSummaryResponse>) (a, b) -> nullSafe(a.sectionName()).compareToIgnoreCase(nullSafe(b.sectionName()))
                    : defaultComparator();
            case "sectionid"   -> isAdmin
                    ? (Comparator<StudentSummaryResponse>) (a, b) -> Long.compare(
                            a.sectionId() != null ? a.sectionId() : Long.MAX_VALUE,
                            b.sectionId() != null ? b.sectionId() : Long.MAX_VALUE)
                    : defaultComparator();
            default            -> defaultComparator();
        };

        return desc ? base.reversed() : base;
    }

    private static Comparator<StudentSummaryResponse> defaultComparator() {
        return (a, b) -> {
            String sA = a.sectionName() != null ? a.sectionName() : "";
            String sB = b.sectionName() != null ? b.sectionName() : "";
            int cmp = sB.compareTo(sA); // sectionName DESC
            if (cmp != 0) return cmp;
            String lA = a.lastName() != null ? a.lastName() : "";
            String lB = b.lastName() != null ? b.lastName() : "";
            int lCmp = lA.compareTo(lB); // lastName ASC
            if (lCmp != 0) return lCmp;
            String fA = a.firstName() != null ? a.firstName() : "";
            String fB = b.firstName() != null ? b.firstName() : "";
            return fA.compareTo(fB); // firstName ASC
        };
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
