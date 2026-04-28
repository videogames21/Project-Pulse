package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.notification.NotificationService;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import edu.tcu.cs.projectpulse.team.dto.TeamRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final NotificationService notificationService;

    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       SectionRepository sectionRepository,
                       NotificationService notificationService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
        this.notificationService = notificationService;
    }

    public TeamEntity findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    public TeamEntity create(TeamRequest request) {
        if (teamRepository.existsByName(request.name())) {
            throw new TeamNameConflictException(request.name());
        }
        TeamEntity team = new TeamEntity();
        team.setName(request.name());
        team.setDescription(request.description());
        team.setWebsiteUrl(request.websiteUrl());
        team.setSectionName(request.sectionName());
        return teamRepository.save(team);
    }

    @Transactional
    public TeamEntity update(Long id, TeamRequest request) {
        TeamEntity team = findById(id);
        if (teamRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new TeamNameConflictException(request.name());
        }
        boolean sectionChanged = !request.sectionName().equals(team.getSectionName());
        if (sectionChanged) {
            userRepository.findByTeamId(id).stream()
                    .filter(u -> u.getRole() == UserRole.STUDENT)
                    .forEach(u -> {
                        u.setTeamId(null);
                        userRepository.save(u);
                    });
        }
        team.setName(request.name());
        team.setDescription(request.description());
        team.setWebsiteUrl(request.websiteUrl());
        team.setSectionName(request.sectionName());
        return teamRepository.save(team);
    }

    public List<TeamEntity> findAll(String sectionName, String teamName) {
        Specification<TeamEntity> spec = Specification
                .where(TeamSpec.hasSectionName(sectionName))
                .and(TeamSpec.hasTeamName(teamName));

        Sort sort = Sort.by(Sort.Order.desc("sectionName"), Sort.Order.asc("name"));
        return teamRepository.findAll(spec, sort);
    }

    @Transactional
    public void delete(Long id) {
        TeamEntity team = findById(id);
        userRepository.findByTeamId(id).forEach(u -> {
            u.setTeamId(null);
            userRepository.save(u);
        });
        teamRepository.delete(team);
    }

    public List<UserEntity> findStudentsByTeamId(Long teamId) {
        return userRepository.findByTeamId(teamId).stream()
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .toList();
    }

    public List<UserEntity> findInstructorsByTeamId(Long teamId) {
        return teamRepository.findInstructorsByTeamId(teamId);
    }

    @Transactional
    public void removeInstructor(Long teamId, Long instructorId) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        UserEntity instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException(instructorId));

        if (instructor.getRole() != UserRole.INSTRUCTOR) {
            throw new IllegalArgumentException("User " + instructorId + " is not an instructor.");
        }

        boolean removed = team.getInstructors().removeIf(i -> i.getId().equals(instructorId));
        if (!removed) {
            throw new IllegalStateException("Instructor is not assigned to this team.");
        }

        teamRepository.save(team);
    }

    @Transactional
    public void assignInstructor(Long teamId, Long instructorId) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        UserEntity instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException(instructorId));

        if (instructor.getRole() != UserRole.INSTRUCTOR) {
            throw new IllegalArgumentException("User " + instructorId + " is not an instructor.");
        }

        // UC-19: instructor must be assigned to the section this team belongs to first
        boolean assignedToSection = sectionRepository.findAllByInstructorId(instructorId)
                .stream()
                .anyMatch(s -> s.getName().equals(team.getSectionName()));
        if (!assignedToSection) {
            throw new IllegalStateException(
                    "Instructor must be assigned to section \"" + team.getSectionName() + "\" before being assigned to a team in that section.");
        }

        if (team.getInstructors().stream().anyMatch(i -> i.getId().equals(instructorId))) {
            throw new IllegalStateException("Instructor is already assigned to this team.");
        }

        team.getInstructors().add(instructor);
        teamRepository.save(team);
    }

    @Transactional
    public void assignStudents(Long teamId, List<Long> studentIds) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        Long teamSectionId = sectionRepository.findByName(team.getSectionName())
                .map(SectionEntity::getId).orElse(null);

        for (Long studentId : studentIds) {
            UserEntity student = userRepository.findById(studentId)
                    .orElseThrow(() -> new UserNotFoundException(studentId));

            if (student.getTeamId() != null) {
                throw new IllegalStateException(
                        "Student " + student.getFirstName() + " " + student.getLastName() + " is already assigned to a team. Remove them first.");
            }

            if (teamSectionId != null && !teamSectionId.equals(student.getSectionId())) {
                throw new IllegalStateException(
                        "Student " + student.getFirstName() + " " + student.getLastName()
                        + " is not enrolled in the section this team belongs to.");
            }

            student.setTeamId(team.getId());
            userRepository.save(student);
        }
    }

    @Transactional
    public void removeStudent(Long teamId, Long studentId) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));

        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("User " + studentId + " is not a student.");
        }

        if (!teamId.equals(student.getTeamId())) {
            throw new IllegalStateException("Student is not a member of this team.");
        }

        student.setTeamId(null);
        userRepository.save(student);

        notificationService.create(studentId,
                "You have been removed from team \"" + team.getName() + "\" by an administrator.");
    }
}
