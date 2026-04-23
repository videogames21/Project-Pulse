package edu.tcu.cs.projectpulse.rubric;

import edu.tcu.cs.projectpulse.rubric.dto.CriterionResponse;
import edu.tcu.cs.projectpulse.rubric.dto.RubricRequest;
import edu.tcu.cs.projectpulse.rubric.dto.RubricResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/active")
    public Result findActive() {
        return new Result(true, StatusCode.SUCCESS, "Active rubric retrieved", toResponse(rubricService.findActive()));
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        return new Result(true, StatusCode.SUCCESS, "Rubric retrieved successfully", toResponse(rubricService.findById(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result create(@Valid @RequestBody RubricRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Rubric created successfully", toResponse(rubricService.create(request)));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody RubricRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Rubric updated successfully", toResponse(rubricService.update(id, request)));
    }

    @PutMapping("/{id}/activate")
    public Result activate(@PathVariable Long id) {
        return new Result(true, StatusCode.SUCCESS, "Rubric activated", toResponse(rubricService.activate(id)));
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
        return new RubricResponse(entity.getId(), entity.getName(), entity.isActive(), criteria);
    }
}
