package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.team.dto.TeamRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
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
}
