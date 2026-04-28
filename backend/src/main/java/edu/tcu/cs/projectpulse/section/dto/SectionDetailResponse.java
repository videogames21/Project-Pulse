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
        Long rubricId,
        String rubricName,
        List<InstructorSummary> instructors
) {
    public record TeamSummary(Long id, String name, List<String> students, List<String> instructors) {}
    public record InstructorSummary(Long id, String firstName, String lastName, String email) {}
}
