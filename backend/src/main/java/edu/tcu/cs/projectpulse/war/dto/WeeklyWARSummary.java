package edu.tcu.cs.projectpulse.war.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyWARSummary(
        LocalDate weekStart,
        List<WARActivityResponse> activities
) {}
