package edu.tcu.cs.projectpulse.war.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamWARReportResponse(
        Long teamId,
        String teamName,
        LocalDate weekStart,
        List<StudentWARSummary> students
) {}
