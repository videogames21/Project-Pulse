package edu.tcu.cs.projectpulse.war.dto;

import java.math.BigDecimal;

public record WAREntryResponse(
        Long id,
        Integer week,
        String category,
        String description,
        BigDecimal plannedHours,
        BigDecimal actualHours,
        String status
) {}
