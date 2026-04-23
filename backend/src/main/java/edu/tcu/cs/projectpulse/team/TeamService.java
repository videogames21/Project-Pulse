package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.notification.NotificationService;
import edu.tcu.cs.projectpulse.user.UserEntity;
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

    public List<TeamEntity> findAll(String sectionName, String teamName) {
        Specification<TeamEntity> spec = Specification
                .where(TeamSpec.hasSectionName(sectionName))
                .and(TeamSpec.hasTeamName(teamName));
        return teamRepository.findAll(spec, Sort.by(Sort.Order.desc("sectionName"), Sort.Order.asc("name")));
    }

    public List<UserEntity> findMembers(Long teamId) {
        return findById(teamId).getMembers();
    }

    public List<UserEntity> findMyTeamMembers(Long userId) {
        return teamRepository.findFirstByMembersId(userId)
                .map(t -> t.getMembers().stream()
                        .filter(m -> !m.getId().equals(userId))
                        .sorted(Comparator.comparing(UserEntity::getLastName))
                        .toList())
                .orElse(List.of());
    }

    public void removeMember(Long teamId, Long userId) {
        TeamEntity team = findById(teamId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));
        team.getMembers().remove(user);
        teamRepository.save(team);
        notificationService.create(userId,
                "You have been removed from team: " + team.getName());
    }
}
