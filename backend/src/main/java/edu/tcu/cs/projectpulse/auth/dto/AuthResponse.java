package edu.tcu.cs.projectpulse.auth.dto;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        String role
) {}
