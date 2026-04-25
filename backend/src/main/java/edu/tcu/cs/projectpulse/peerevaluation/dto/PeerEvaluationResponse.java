package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.time.LocalDate;
import java.util.List;

public record PeerEvaluationResponse(
        Long id,
        Long evaluatorId,
        Long evaluateeId,
        LocalDate weekStart,
        List<ScoreResponse> scores,
        String publicComments,
        String privateComments
) {}
