package edu.tcu.cs.projectpulse.section.dto;

import java.time.LocalDate;
import java.util.List;

public record SectionDetailResponse(
        Long id,
        String sectionName,
        LocalDate startDate,
        LocalDate endDate,
        List<TeamSummary> teams,
        List<String> instructorsNotOnTeam,
        List<String> studentsNotOnTeam,
        String rubricName
) {
    public record TeamSummary(Long id, String name) {}
}
