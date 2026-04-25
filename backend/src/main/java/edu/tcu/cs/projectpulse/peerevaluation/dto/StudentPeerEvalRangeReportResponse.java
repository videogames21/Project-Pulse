package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.time.LocalDate;
import java.util.List;

public record StudentPeerEvalRangeReportResponse(
        Long studentId,
        String studentName,
        LocalDate startWeek,
        LocalDate endWeek,
        List<WeeklyPeerEvalSummary> weeks
) {}
