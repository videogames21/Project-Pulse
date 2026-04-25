package edu.tcu.cs.projectpulse.war.dto;

import edu.tcu.cs.projectpulse.war.ActivityCategory;
import edu.tcu.cs.projectpulse.war.ActivityStatus;

import java.math.BigDecimal;

public record WARActivityResponse(
        Long id,
        ActivityCategory category,
        String description,
        BigDecimal plannedHours,
        BigDecimal actualHours,
        ActivityStatus status
) {}
