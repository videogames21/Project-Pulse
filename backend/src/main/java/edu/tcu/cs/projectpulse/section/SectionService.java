package edu.tcu.cs.projectpulse.section;

import edu.tcu.cs.projectpulse.section.dto.SectionDetailResponse;
import edu.tcu.cs.projectpulse.section.dto.SectionResponse;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public SectionService(SectionRepository sectionRepository, TeamRepository teamRepository,
                          UserRepository userRepository) {
        this.sectionRepository = sectionRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public List<SectionResponse> findAll(String name) {
        Specification<SectionEntity> spec = Specification.where(SectionSpec.hasName(name));
        Sort sort = Sort.by(Sort.Order.desc("name"));

        return sectionRepository.findAll(spec, sort)
                .stream()
                .map(section -> {
                    List<String> teamNames = teamRepository
                            .findAllBySectionNameOrderByNameAsc(section.getName())
                            .stream()
                            .map(TeamEntity::getName)
                            .toList();
                    return new SectionResponse(
                            section.getId(),
                            section.getName(),
                            section.getStartDate(),
                            section.getEndDate(),
                            teamNames
                    );
                })
                .toList();
    }

    public SectionDetailResponse findSectionById(Long id) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        List<SectionDetailResponse.TeamSummary> teams = teamRepository
                .findAllBySectionNameOrderByNameAsc(section.getName())
                .stream()
                .map(t -> new SectionDetailResponse.TeamSummary(t.getId(), t.getName()))
                .toList();

        List<String> studentsNotOnTeam = userRepository
                .findByRoleAndTeamIdIsNull(UserRole.STUDENT)
                .stream()
                .map(UserEntity::getName)
                .sorted()
                .toList();

        return new SectionDetailResponse(
                section.getId(),
                section.getName(),
                section.getStartDate(),
                section.getEndDate(),
                teams,
                List.of(),
                studentsNotOnTeam,
                null
        );
    }
}
