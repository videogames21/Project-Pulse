package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;

public record CriterionAverageScoreResponse(Long criterionId, String criterionName, BigDecimal averageScore) {}
