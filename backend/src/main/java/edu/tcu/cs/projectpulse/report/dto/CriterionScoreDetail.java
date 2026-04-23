package edu.tcu.cs.projectpulse.report.dto;

import java.math.BigDecimal;

public record CriterionScoreDetail(
        Long criterionId,
        String name,
        Integer score,
        BigDecimal maxScore
) {}
