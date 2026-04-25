package edu.tcu.cs.projectpulse.peerevaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
        @NotNull(message = "Criterion ID is required")
        Long criterionId,

        @NotNull(message = "Score is required")
        @Min(value = 1, message = "Score must be at least 1")
        @Max(value = 10, message = "Score must be at most 10")
        Integer score
) {}
