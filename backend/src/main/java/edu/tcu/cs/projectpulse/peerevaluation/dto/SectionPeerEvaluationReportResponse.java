package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SectionPeerEvaluationReportResponse(
        String sectionName,
        LocalDate weekStart,
        BigDecimal maxGrade,
        List<StudentEvaluationSummary> students
) {}
