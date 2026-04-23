package edu.tcu.cs.projectpulse.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record WeekWARDetail(
        Integer week,
        List<ActivityDetail> activities,
        BigDecimal totalPlanned,
        BigDecimal totalActual
) {}
