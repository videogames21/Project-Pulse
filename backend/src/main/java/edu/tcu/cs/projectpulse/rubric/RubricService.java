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

    @Transactional(readOnly = true)
    public RubricEntity findActive() {
        return rubricRepository.findByActiveTrue()
                .orElseThrow(() -> new RubricNotFoundException("No active rubric found. Activate one via PUT /api/v1/rubrics/{id}/activate"));
    }

    public RubricEntity create(RubricRequest request) {
        if (rubricRepository.existsByName(request.name())) {
            throw new RubricNameConflictException(request.name());
        }

        RubricEntity rubric = new RubricEntity();
        rubric.setName(request.name());
        applyRequest(rubric, request);
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
        applyRequest(rubric, request);
        return rubricRepository.save(rubric);
    }

    public RubricEntity activate(Long id) {
        rubricRepository.findAll().forEach(r -> {
            r.setActive(false);
            rubricRepository.save(r);
        });
        RubricEntity rubric = findById(id);
        rubric.setActive(true);
        return rubricRepository.save(rubric);
    }

    public void delete(Long id) {
        RubricEntity rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new RubricNotFoundException(id));
        rubricRepository.delete(rubric);
    }

    private void applyRequest(RubricEntity rubric, RubricRequest request) {
        for (CriterionRequest cr : request.criteria()) {
            CriterionEntity criterion = new CriterionEntity();
            criterion.setName(cr.name());
            criterion.setDescription(cr.description());
            criterion.setMaxScore(cr.maxScore());
            rubric.addCriterion(criterion);
        }
    }
}
