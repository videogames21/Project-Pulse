package edu.tcu.cs.projectpulse.student.dto;

public record StudentSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Long teamId,
        String teamName,
        Long sectionId,
        String sectionName
) {}
