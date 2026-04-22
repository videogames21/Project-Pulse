package edu.tcu.cs.projectpulse.team;

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

    public List<TeamEntity> findAll(String sectionName, String teamName) {
        Specification<TeamEntity> spec = Specification
                .where(TeamSpec.hasSectionName(sectionName))
                .and(TeamSpec.hasTeamName(teamName));

        Sort sort = Sort.by(Sort.Order.desc("sectionName"), Sort.Order.asc("name"));
        return teamRepository.findAll(spec, sort);
    }
}
