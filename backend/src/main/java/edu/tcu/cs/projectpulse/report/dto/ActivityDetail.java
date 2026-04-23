package edu.tcu.cs.projectpulse.report.dto;

import java.math.BigDecimal;

public record ActivityDetail(
        Long id,
        String category,
        String description,
        BigDecimal plannedHours,
        BigDecimal actualHours,
        String status
) {}
