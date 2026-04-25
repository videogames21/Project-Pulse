package edu.tcu.cs.projectpulse.invitation.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteInstructorsRequest(
        @NotBlank(message = "emails is required")
        String emails
) {}
