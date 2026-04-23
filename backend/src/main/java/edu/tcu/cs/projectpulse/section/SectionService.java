package edu.tcu.cs.projectpulse.section;

import edu.tcu.cs.projectpulse.section.dto.SectionResponse;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;

    public SectionService(SectionRepository sectionRepository, TeamRepository teamRepository) {
        this.sectionRepository = sectionRepository;
        this.teamRepository = teamRepository;
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
                            .map(t -> t.getName())
                            .toList();
                    return new SectionResponse(section.getName(), teamNames);
                })
                .toList();
    }

    public SectionEntity findById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));
    }
}
