package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentEvaluationSummary(
        Long studentId,
        String studentName,
        boolean didSubmit,
        int evaluationCount,
        BigDecimal averageGrade,
        List<ReceivedEvaluationDetail> receivedEvaluations
) {}
