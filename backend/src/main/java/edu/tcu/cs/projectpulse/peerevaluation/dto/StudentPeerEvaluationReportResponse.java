package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StudentPeerEvaluationReportResponse(
        Long studentId,
        String studentName,
        LocalDate weekStart,
        int evaluationCount,
        List<CriterionAverageScoreResponse> averageCriterionScores,
        List<String> publicComments,
        BigDecimal averageGrade
) {}
