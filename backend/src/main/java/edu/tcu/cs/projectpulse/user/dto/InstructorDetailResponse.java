package edu.tcu.cs.projectpulse.user.dto;

import java.util.List;

public record InstructorDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        List<SupervisedTeam> supervisedTeams,
        String supervisedSectionName,
        Long supervisedSectionId
) {
    public record SupervisedTeam(Long teamId, String teamName, String sectionName) {}
}
