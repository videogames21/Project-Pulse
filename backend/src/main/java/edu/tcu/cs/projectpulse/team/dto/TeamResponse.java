package edu.tcu.cs.projectpulse.team.dto;

public record TeamResponse(
        Long id,
        String name,
        String description,
        String websiteUrl,
        String sectionName
) {}
