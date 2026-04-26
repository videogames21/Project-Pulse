package edu.tcu.cs.projectpulse.student.dto;

import java.time.LocalDate;

public record StudentWAREntry(
        Long warId,
        LocalDate weekStart,
        int activityCount,
        String teamName,
        String sectionName
) {}
