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

        // Resolve section constraint
        Long effectiveSectionId = null;

        if (!isAdmin) {
            // Instructor: force-filter to their own section only
            Optional<SectionEntity> section = sectionRepository.findAllByInstructorId(caller.getId()).stream().findFirst();
            if (section.isEmpty()) return List.of();
            effectiveSectionId = section.get().getId();
        } else {
            // Admin: apply optional section filter
            if (sectionId != null) {
                Optional<SectionEntity> sec = sectionRepository.findById(sectionId);
                if (sec.isEmpty()) return List.of();
                effectiveSectionId = sec.get().getId();
                sectionName = sec.get().getName();
            } else if (sectionName != null && !sectionName.isBlank()) {
                Optional<SectionEntity> sec = sectionRepository.findByName(sectionName);
                if (sec.isEmpty()) return List.of();
                effectiveSectionId = sec.get().getId();
            }
        }

        // Resolve teamName filter to allowed team IDs
        Set<Long> allowedTeamIds = null;
        if (teamName != null && !teamName.isBlank()) {
            String tn = teamName.toLowerCase();
            List<TeamEntity> matchingTeams = teamRepository.findAll().stream()
                    .filter(t -> t.getName().toLowerCase().contains(tn))
                    .toList();
            allowedTeamIds = matchingTeams.stream()
                    .map(TeamEntity::getId).collect(Collectors.toSet());
        }

        // Build JPA specification
        Specification<UserEntity> spec = Specification.where(StudentSpec.isStudent())
                .and(StudentSpec.hasFirstName(firstName))
                .and(StudentSpec.hasLastName(lastName))
                .and(StudentSpec.hasEmail(email))
                .and(StudentSpec.hasSectionId(effectiveSectionId));

        if (teamId != null) {
            if (allowedTeamIds != null && !allowedTeamIds.contains(teamId)) {
                return List.of();
            }
            spec = spec.and(StudentSpec.hasTeamId(teamId));
        } else if (allowedTeamIds != null) {
            if (allowedTeamIds.isEmpty()) return List.of();
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

    @Transactional(readOnly = true)
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
        if (student.getTeamId() != null) {
            teamName = teamRepository.findById(student.getTeamId())
                    .map(TeamEntity::getName).orElse(null);
        }
        Long sectionId = student.getSectionId();
        String sectionName = sectionId != null
                ? sectionRepository.findById(sectionId).map(SectionEntity::getName).orElse(null)
                : null;

        String resolvedTeamName    = teamName    != null ? teamName    : "Unknown";
        String resolvedSectionName = sectionName != null ? sectionName : "Unknown";

        // WARs
        List<StudentWAREntry> wars = warRepository.findAllByStudentId(id).stream()
                .sorted(Comparator.comparing(WAREntity::getWeekStart))
                .map(war -> new StudentWAREntry(war.getId(), war.getWeekStart(),
                        war.getActivities().size(), resolvedTeamName, resolvedSectionName))
                .toList();

        // Peer evals received
        List<StudentPeerEvalEntry> evals = peerEvaluationRepository.findAllByEvaluateeId(id).stream()
                .sorted(Comparator.comparing(PeerEvaluationEntity::getWeekStart))
                .map(eval -> {
                    String evaluatorName = userRepository.findById(eval.getEvaluatorId())
                            .map(UserEntity::getName).orElse("Unknown");
                    int totalScore = eval.getScores().stream()
                            .mapToInt(s -> s.getScore() != null ? s.getScore() : 0).sum();
                    return new StudentPeerEvalEntry(eval.getId(), eval.getWeekStart(),
                            eval.getEvaluatorId(), evaluatorName, totalScore,
                            resolvedTeamName, resolvedSectionName);
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

        // Delete peer evaluations per-entity so CascadeType.ALL fires for peer_evaluation_scores
        Set<PeerEvaluationEntity> evalsToDelete = new HashSet<>();
        evalsToDelete.addAll(peerEvaluationRepository.findAllByEvaluatorId(id));
        evalsToDelete.addAll(peerEvaluationRepository.findAllByEvaluateeId(id));
        evalsToDelete.forEach(peerEvaluationRepository::delete);

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
        boolean authorized = sectionRepository.findAllByInstructorId(instructor.getId())
                .stream()
                .anyMatch(s -> s.getId().equals(student.getSectionId()));
        if (!authorized) {
            throw new AccessDeniedException("You are not authorized to view this student.");
        }
    }

    private StudentSummaryResponse toSummary(UserEntity student) {
        String teamName = null;
        if (student.getTeamId() != null) {
            teamName = teamRepository.findById(student.getTeamId())
                    .map(TeamEntity::getName).orElse(null);
        }
        Long sectionId = student.getSectionId();
        String sectionName = sectionId != null
                ? sectionRepository.findById(sectionId).map(SectionEntity::getName).orElse(null)
                : null;
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
