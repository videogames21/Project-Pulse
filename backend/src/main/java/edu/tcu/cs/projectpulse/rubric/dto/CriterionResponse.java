package edu.tcu.cs.projectpulse.rubric.dto;

import java.math.BigDecimal;

public record CriterionResponse(
        Long id,
        String name,
        String description,
        BigDecimal maxScore
) {}
