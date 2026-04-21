package edu.tcu.cs.projectpulse.rubric;

import edu.tcu.cs.projectpulse.rubric.dto.CriterionResponse;
import edu.tcu.cs.projectpulse.rubric.dto.RubricRequest;
import edu.tcu.cs.projectpulse.rubric.dto.RubricResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rubrics")
public class RubricController {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @GetMapping
    public Result findAll() {
        List<RubricResponse> rubrics = rubricService.findAll().stream()
                .map(this::toResponse)
                .toList();
        return new Result(true, StatusCode.SUCCESS, "Rubrics retrieved successfully", rubrics);
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        RubricResponse rubric = toResponse(rubricService.findById(id));
        return new Result(true, StatusCode.SUCCESS, "Rubric retrieved successfully", rubric);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result create(@Valid @RequestBody RubricRequest request) {
        RubricResponse rubric = toResponse(rubricService.create(request));
        return new Result(true, StatusCode.SUCCESS, "Rubric created successfully", rubric);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody RubricRequest request) {
        RubricResponse rubric = toResponse(rubricService.update(id, request));
        return new Result(true, StatusCode.SUCCESS, "Rubric updated successfully", rubric);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        rubricService.delete(id);
        return new Result(true, StatusCode.SUCCESS, "Rubric deleted successfully");
    }

    private RubricResponse toResponse(RubricEntity entity) {
        List<CriterionResponse> criteria = entity.getCriteria().stream()
                .map(c -> new CriterionResponse(c.getId(), c.getName(), c.getDescription(), c.getMaxScore()))
                .toList();
        return new RubricResponse(entity.getId(), entity.getName(), criteria);
    }
}
