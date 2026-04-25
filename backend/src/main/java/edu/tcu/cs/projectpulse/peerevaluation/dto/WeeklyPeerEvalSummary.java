package edu.tcu.cs.projectpulse.peerevaluation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyPeerEvalSummary(
        LocalDate weekStart,
        int evaluationCount,
        BigDecimal averageGrade,
        List<ReceivedEvaluationDetail> receivedEvaluations
) {}
