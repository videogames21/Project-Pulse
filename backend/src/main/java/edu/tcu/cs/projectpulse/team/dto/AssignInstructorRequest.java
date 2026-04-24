package edu.tcu.cs.projectpulse.team.dto;

import jakarta.validation.constraints.NotNull;

public record AssignInstructorRequest(
        @NotNull(message = "instructorId is required")
        Long instructorId
) {}
