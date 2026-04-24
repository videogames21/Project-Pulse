package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.user.UserRepository;
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

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
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
        return userRepository.findByTeamId(teamId);
    }

    @Transactional
    public void assignStudents(Long teamId, List<Long> studentIds) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        for (Long studentId : studentIds) {
            UserEntity student = userRepository.findById(studentId)
                    .orElseThrow(() -> new UserNotFoundException(studentId));

            if (student.getTeamId() != null) {
                throw new IllegalStateException(
                        "Student " + student.getName() + " is already assigned to a team. Remove them first.");
            }

            student.setTeamId(team.getId());
            userRepository.save(student);
        }
    }

    @Transactional
    public void removeStudent(Long teamId, Long studentId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));

        if (!teamId.equals(student.getTeamId())) {
            throw new IllegalStateException("Student is not a member of this team.");
        }

        student.setTeamId(null);
        userRepository.save(student);
    }
}
