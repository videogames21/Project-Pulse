package edu.tcu.cs.projectpulse.rubric;

import edu.tcu.cs.projectpulse.rubric.dto.CriterionRequest;
import edu.tcu.cs.projectpulse.rubric.dto.RubricRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RubricService {

    private final RubricRepository rubricRepository;

    public RubricService(RubricRepository rubricRepository) {
        this.rubricRepository = rubricRepository;
    }

    @Transactional(readOnly = true)
    public List<RubricEntity> findAll() {
        return rubricRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RubricEntity findById(Long id) {
        return rubricRepository.findById(id)
                .orElseThrow(() -> new RubricNotFoundException(id));
    }

    public RubricEntity create(RubricRequest request) {
        if (rubricRepository.existsByName(request.name())) {
            throw new RubricNameConflictException(request.name());
        }

        RubricEntity rubric = new RubricEntity();
        rubric.setName(request.name());

        for (CriterionRequest cr : request.criteria()) {
            CriterionEntity criterion = new CriterionEntity();
            criterion.setName(cr.name());
            criterion.setDescription(cr.description());
            criterion.setMaxScore(cr.maxScore());
            rubric.addCriterion(criterion);
        }

        return rubricRepository.save(rubric);
    }

    public RubricEntity update(Long id, RubricRequest request) {
        RubricEntity rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new RubricNotFoundException(id));

        if (!rubric.getName().equals(request.name()) && rubricRepository.existsByName(request.name())) {
            throw new RubricNameConflictException(request.name());
        }

        rubric.setName(request.name());
        rubric.getCriteria().clear();

        for (CriterionRequest cr : request.criteria()) {
            CriterionEntity criterion = new CriterionEntity();
            criterion.setName(cr.name());
            criterion.setDescription(cr.description());
            criterion.setMaxScore(cr.maxScore());
            rubric.addCriterion(criterion);
        }

        return rubricRepository.save(rubric);
    }

    public void delete(Long id) {
        RubricEntity rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new RubricNotFoundException(id));
        rubricRepository.delete(rubric);
    }
}
