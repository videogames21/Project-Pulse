package edu.tcu.cs.projectpulse.invitation.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateStudentInvitationRequest(
        @NotNull(message = "Section ID is required") Long sectionId
) {}
