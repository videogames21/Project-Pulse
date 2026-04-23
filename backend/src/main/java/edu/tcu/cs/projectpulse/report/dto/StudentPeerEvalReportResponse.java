package edu.tcu.cs.projectpulse.report.dto;

import java.util.List;

public record StudentPeerEvalReportResponse(
        Integer week,
        double overallScore,
        double maxScore,
        List<CriterionAvgResponse> criteria,
        List<String> publicComments
) {}
