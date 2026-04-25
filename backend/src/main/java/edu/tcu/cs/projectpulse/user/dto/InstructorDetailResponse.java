package edu.tcu.cs.projectpulse.user.dto;

public record InstructorDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        SupervisedTeam supervisedTeam
) {
    public record SupervisedTeam(Long teamId, String teamName, String sectionName) {}
}
