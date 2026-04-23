package edu.tcu.cs.projectpulse.report.dto;

import java.util.List;

public record EvaluationDetail(
        Long evaluatorId,
        String evaluatorName,
        int totalScore,
        List<CriterionScoreDetail> criterionScores,
        String publicComment,
        String privateComment
) {}
