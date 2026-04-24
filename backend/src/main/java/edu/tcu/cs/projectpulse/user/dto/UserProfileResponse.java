package edu.tcu.cs.projectpulse.user.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String role,
        Long teamId,
        String teamName,
        String sectionName,
        String instructorName,
        String instructorEmail
) {}
