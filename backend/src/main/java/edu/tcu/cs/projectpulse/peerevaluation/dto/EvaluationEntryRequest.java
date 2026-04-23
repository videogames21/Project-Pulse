package edu.tcu.cs.projectpulse.peerevaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EvaluationEntryRequest(
        @NotNull(message = "Evaluatee ID is required")
        Long evaluateeId,

        @NotEmpty(message = "Criterion scores are required")
        @Valid
        List<CriterionScoreRequest> criterionScores,

        String publicComment,
        String privateComment
) {}
