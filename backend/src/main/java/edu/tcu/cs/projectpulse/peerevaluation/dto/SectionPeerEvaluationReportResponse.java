package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.time.LocalDate;
import java.util.List;

public record SectionPeerEvaluationReportResponse(
        String sectionName,
        LocalDate weekStart,
        List<StudentEvaluationSummary> students
) {}
