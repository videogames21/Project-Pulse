package edu.tcu.cs.projectpulse.peerevaluation.dto;

import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
        @NotNull(message = "Criterion ID is required")
        Long criterionId,

        @NotNull(message = "Score is required")
        Integer score
) {}
