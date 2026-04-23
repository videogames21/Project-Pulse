package edu.tcu.cs.projectpulse.report.dto;

public record SectionPeerEvalRow(
        Long studentId,
        String firstName,
        String lastName,
        boolean submitted,
        double totalScore,
        double maxScore,
        int evaluationsReceived,
        int teamSize
) {}
