package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;

public record ReceivedEvaluationDetail(
        Long evaluatorId,
        String evaluatorName,
        BigDecimal totalScore,
        String publicComments,
        String privateComments
) {}
