package edu.tcu.cs.projectpulse.report.dto;

import java.math.BigDecimal;

public record CriterionAvgResponse(
        Long criterionId,
        String name,
        String description,
        double avgScore,
        BigDecimal maxScore
) {}
