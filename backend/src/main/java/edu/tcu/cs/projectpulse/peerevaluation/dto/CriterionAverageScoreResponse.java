package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;

public record CriterionAverageScoreResponse(Long criterionId, BigDecimal averageScore) {}
