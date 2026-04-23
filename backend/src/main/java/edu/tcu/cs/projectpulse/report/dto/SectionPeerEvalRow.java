package edu.tcu.cs.projectpulse.report.dto;

import java.util.List;

public record SectionPeerEvalRow(
        Long studentId,
        String firstName,
        String lastName,
        boolean submitted,
        double totalScore,
        double maxScore,
        int evaluationsReceived,
        int teamSize,
        List<SectionEvalComment> comments
) {}
