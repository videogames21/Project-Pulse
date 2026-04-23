package edu.tcu.cs.projectpulse.peerevaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitPeerEvalRequest(
        @NotNull(message = "Week is required")
        Integer week,

        @NotEmpty(message = "At least one evaluation is required")
        @Valid
        List<EvaluationEntryRequest> evaluations
) {}
