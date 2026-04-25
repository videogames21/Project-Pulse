package edu.tcu.cs.projectpulse.war.dto;

import java.util.List;

public record StudentWARSummary(
        Long studentId,
        String studentName,
        boolean didSubmit,
        List<WARActivityResponse> activities
) {}
