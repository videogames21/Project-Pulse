package edu.tcu.cs.projectpulse.student.dto;

import java.time.LocalDate;

public record StudentPeerEvalEntry(
        Long evalId,
        LocalDate weekStart,
        Long evaluatorId,
        String evaluatorName,
        int totalScore,
        String teamName,
        String sectionName
) {}
