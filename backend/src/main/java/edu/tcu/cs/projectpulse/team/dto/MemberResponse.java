package edu.tcu.cs.projectpulse.team.dto;

public record MemberResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {}
