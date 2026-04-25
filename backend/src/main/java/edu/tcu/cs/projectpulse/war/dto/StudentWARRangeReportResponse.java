package edu.tcu.cs.projectpulse.war.dto;

import java.time.LocalDate;
import java.util.List;

public record StudentWARRangeReportResponse(
        Long studentId,
        String studentName,
        LocalDate startWeek,
        LocalDate endWeek,
        List<WeeklyWARSummary> weeks
) {}
