package edu.tcu.cs.projectpulse.section;

import edu.tcu.cs.projectpulse.activeweek.ActiveWeekRepository;
import edu.tcu.cs.projectpulse.rubric.CriterionEntity;
import edu.tcu.cs.projectpulse.rubric.RubricEntity;
import edu.tcu.cs.projectpulse.rubric.RubricNotFoundException;
import edu.tcu.cs.projectpulse.rubric.RubricRepository;
import edu.tcu.cs.projectpulse.rubric.dto.CriterionRequest;
import edu.tcu.cs.projectpulse.section.dto.SectionDetailResponse;
import edu.tcu.cs.projectpulse.section.dto.SectionRequest;
import edu.tcu.cs.projectpulse.section.dto.SectionResponse;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RubricRepository rubricRepository;
    private final ActiveWeekRepository activeWeekRepository;

    public SectionService(SectionRepository sectionRepository, TeamRepository teamRepository,
                          UserRepository userRepository, RubricRepository rubricRepository,
                          ActiveWeekRepository activeWeekRepository) {
        this.sectionRepository = sectionRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.rubricRepository = rubricRepository;
        this.activeWeekRepository = activeWeekRepository;
    }

    @Transactional
    public SectionDetailResponse create(SectionRequest request) {
        if (sectionRepository.existsByName(request.name())) {
            throw new SectionNameConflictException(request.name());
        }

        SectionEntity section = new SectionEntity();
        section.setName(request.name());
        section.setStartDate(request.startDate());
        section.setEndDate(request.endDate());

        if (request.rubricId() != null) {
            RubricEntity original = rubricRepository.findById(request.rubricId())
                    .orElseThrow(() -> new RubricNotFoundException(request.rubricId()));

            if (request.criteria() != null && !request.criteria().isEmpty()) {
                RubricEntity copy = new RubricEntity();
                String baseName = "Copy of " + original.getName();
                String copyName = baseName;
                int suffix = 2;
                while (rubricRepository.existsByName(copyName)) {
                    copyName = baseName + " (" + suffix++ + ")";
                }
                copy.setName(copyName);
                for (CriterionRequest cr : request.criteria()) {
                    CriterionEntity criterion = new CriterionEntity();
                    criterion.setName(cr.name());
                    criterion.setDescription(cr.description());
                    criterion.setMaxScore(cr.maxScore());
                    copy.addCriterion(criterion);
                }
                RubricEntity saved = rubricRepository.save(copy);
                section.setRubricId(saved.getId());
            } else {
                section.setRubricId(original.getId());
            }
        }

        SectionEntity saved = sectionRepository.save(section);
        return findSectionById(saved.getId());
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

    @Transactional
    public SectionDetailResponse update(Long id, SectionRequest request) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        if (sectionRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new SectionNameConflictException(request.name());
        }

        String oldName = section.getName();
        String newName = request.name();

        section.setName(newName);
        section.setStartDate(request.startDate());
        section.setEndDate(request.endDate());

        if (!oldName.equals(newName)) {
            teamRepository.findAllBySectionNameOrderByNameAsc(oldName)
                    .forEach(t -> t.setSectionName(newName));
        }

        if (request.rubricId() != null) {
            RubricEntity original = rubricRepository.findById(request.rubricId())
                    .orElseThrow(() -> new RubricNotFoundException(request.rubricId()));

            if (request.criteria() != null && !request.criteria().isEmpty()) {
                RubricEntity copy = new RubricEntity();
                String baseName = "Copy of " + original.getName();
                String copyName = baseName;
                int suffix = 2;
                while (rubricRepository.existsByName(copyName)) {
                    copyName = baseName + " (" + suffix++ + ")";
                }
                copy.setName(copyName);
                for (CriterionRequest cr : request.criteria()) {
                    CriterionEntity criterion = new CriterionEntity();
                    criterion.setName(cr.name());
                    criterion.setDescription(cr.description());
                    criterion.setMaxScore(cr.maxScore());
                    copy.addCriterion(criterion);
                }
                RubricEntity saved = rubricRepository.save(copy);
                section.setRubricId(saved.getId());
            } else {
                section.setRubricId(original.getId());
            }
        } else {
            section.setRubricId(null);
        }

        sectionRepository.save(section);
        activeWeekRepository.deleteOutOfRange(id, request.startDate(), request.endDate());
        return findSectionById(id);
    }

    public SectionDetailResponse findSectionById(Long id) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        List<SectionDetailResponse.TeamSummary> teams = teamRepository
                .findAllBySectionNameOrderByNameAsc(section.getName())
                .stream()
                .map(t -> {
                    List<UserEntity> members = userRepository.findByTeamId(t.getId());
                    List<String> students = members.stream()
                            .filter(u -> u.getRole() == UserRole.STUDENT)
                            .map(u -> u.getFirstName() + " " + u.getLastName())
                            .sorted()
                            .toList();
                    List<String> instructors = members.stream()
                            .filter(u -> u.getRole() == UserRole.INSTRUCTOR)
                            .map(u -> u.getFirstName() + " " + u.getLastName())
                            .sorted()
                            .toList();
                    return new SectionDetailResponse.TeamSummary(t.getId(), t.getName(), students, instructors);
                })
                .toList();

        List<String> studentsNotOnTeam = userRepository
                .findByRoleAndTeamIdIsNull(UserRole.STUDENT)
                .stream()
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .sorted()
                .toList();

        List<String> instructorsNotOnTeam = userRepository
                .findByRoleAndTeamIdIsNull(UserRole.INSTRUCTOR)
                .stream()
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .sorted()
                .toList();

        String rubricName = null;
        if (section.getRubricId() != null) {
            rubricName = rubricRepository.findById(section.getRubricId())
                    .map(r -> r.getName())
                    .orElse(null);
        }

        return new SectionDetailResponse(
                section.getId(),
                section.getName(),
                section.getStartDate(),
                section.getEndDate(),
                teams,
                instructorsNotOnTeam,
                studentsNotOnTeam,
                section.getRubricId(),
                rubricName
        );
    }

    public RubricEntity getRubricForSection(String sectionName) {
        SectionEntity section = sectionRepository.findByName(sectionName)
                .orElseThrow(() -> new SectionNotFoundException(sectionName));
        if (section.getRubricId() == null) {
            throw new IllegalStateException("Section \"" + sectionName + "\" has no rubric assigned.");
        }
        return rubricRepository.findById(section.getRubricId())
                .orElseThrow(() -> new RubricNotFoundException(section.getRubricId()));
    }

    @Transactional
    public void delete(Long id) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));
        activeWeekRepository.deleteBySectionId(id);
        sectionRepository.delete(section);
    }
}
