package edu.tcu.cs.projectpulse.rubric.dto;

import java.util.List;

public record RubricResponse(
        Long id,
        String name,
        boolean active,
        List<CriterionResponse> criteria
) {}
