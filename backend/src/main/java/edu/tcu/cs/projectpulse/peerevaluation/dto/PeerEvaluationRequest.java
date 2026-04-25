package edu.tcu.cs.projectpulse.peerevaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PeerEvaluationRequest(
        @NotNull(message = "Evaluator ID is required")
        Long evaluatorId,

        @NotNull(message = "Evaluatee ID is required")
        Long evaluateeId,

        @NotNull(message = "Week start date is required")
        LocalDate weekStart,

        @NotEmpty(message = "At least one score is required")
        @Valid
        List<ScoreRequest> scores,

        String publicComments,
        String privateComments
) {}
