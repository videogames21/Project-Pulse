package edu.tcu.cs.projectpulse.team.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamRequest(
        @NotBlank(message = "Team name is required")
        String name,

        String description,

        String websiteUrl,

        @NotBlank(message = "Section name is required")
        String sectionName
) {}
