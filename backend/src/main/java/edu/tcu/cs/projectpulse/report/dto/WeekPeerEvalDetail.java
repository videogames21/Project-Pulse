package edu.tcu.cs.projectpulse.report.dto;

import java.util.List;

public record WeekPeerEvalDetail(
        Integer week,
        double overallScore,
        double maxScore,
        List<CriterionAvgResponse> criteria,
        List<EvaluationDetail> evaluations
) {}
