package edu.tcu.cs.projectpulse.user.dto;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        Long teamId
) {}
