package edu.tcu.cs.projectpulse.user.dto;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        Long teamId,
        String teamName,
        String supervisedSectionName
) {}
