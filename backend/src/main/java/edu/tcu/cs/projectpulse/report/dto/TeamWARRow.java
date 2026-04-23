package edu.tcu.cs.projectpulse.report.dto;

import java.math.BigDecimal;

public record TeamWARRow(
        Long studentId,
        String firstName,
        String lastName,
        int activityCount,
        BigDecimal plannedHours,
        BigDecimal actualHours
) {}
