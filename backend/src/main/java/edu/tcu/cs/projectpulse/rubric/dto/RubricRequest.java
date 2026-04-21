package edu.tcu.cs.projectpulse.rubric.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RubricRequest(
        @NotBlank(message = "Rubric name is required")
        String name,

        @NotEmpty(message = "At least one criterion is required")
        @Valid
        List<CriterionRequest> criteria
) {}
