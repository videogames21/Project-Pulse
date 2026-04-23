package edu.tcu.cs.projectpulse.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeamWARRow(
        Long studentId,
        String firstName,
        String lastName,
        int activityCount,
        BigDecimal plannedHours,
        BigDecimal actualHours,
        List<ActivityDetail> activities
) {}
