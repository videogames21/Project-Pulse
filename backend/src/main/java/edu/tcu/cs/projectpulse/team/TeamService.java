package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.notification.NotificationService;
import edu.tcu.cs.projectpulse.team.dto.TeamRequest;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository,
                       NotificationService notificationService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
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

    public TeamEntity update(Long id, TeamRequest request) {
        TeamEntity team = findById(id);
        if (teamRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new TeamNameConflictException(request.name());
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
        return teamRepository.findAll(spec, Sort.by(Sort.Order.desc("sectionName"), Sort.Order.asc("name")));
    }

    public void delete(Long id) {
        TeamEntity team = findById(id);
        team.getMembers().clear();
        teamRepository.save(team);
        teamRepository.delete(team);
    }

    public List<UserEntity> findMembers(Long teamId) {
        return findById(teamId).getMembers();
    }

    public void assignStudents(Long teamId, List<Long> studentIds) {
        TeamEntity team = findById(teamId);
        for (Long studentId : studentIds) {
            UserEntity student = userRepository.findById(studentId)
                    .orElseThrow(() -> new UserNotFoundException(studentId));
            boolean alreadyOnATeam = teamRepository.findFirstByMembersId(studentId).isPresent();
            if (alreadyOnATeam) {
                throw new IllegalStateException(
                        "Student " + student.getFirstName() + " " + student.getLastName()
                        + " is already assigned to a team. Remove them first.");
            }
            team.getMembers().add(student);
        }
        teamRepository.save(team);
    }

    public List<UserEntity> findMyTeamMembers(Long userId) {
        return teamRepository.findFirstByMembersId(userId)
                .map(t -> t.getMembers().stream()
                        .sorted(Comparator.comparing(UserEntity::getLastName))
                        .toList())
                .orElse(List.of());
    }

    public void removeMember(Long teamId, Long userId) {
        TeamEntity team = findById(teamId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        team.getMembers().remove(user);
        teamRepository.save(team);
        notificationService.create(userId,
                "You have been removed from team: " + team.getName());
    }
}
