package edu.tcu.cs.projectpulse.peerevaluation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CriterionScoreRequest(
        @NotNull(message = "Criterion ID is required")
        Long criterionId,

        @NotNull(message = "Score is required")
        @Min(value = 0, message = "Score cannot be negative")
        Integer score
) {}
