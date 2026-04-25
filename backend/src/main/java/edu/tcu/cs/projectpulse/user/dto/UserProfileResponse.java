package edu.tcu.cs.projectpulse.user.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String firstName,
        String lastName,
        String middleInitial,
        String email,
        String role,
        // student-only fields
        Long teamId,
        String teamName,
        String sectionName,
        String instructorName,
        String instructorEmail,
        // instructor-only fields
        String supervisedSectionName,
        String adminName,
        String adminEmail
) {}
