package edu.tcu.cs.projectpulse.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeactivateRequest(
        @NotBlank(message = "A reason for deactivation is required")
        String reason
) {}
