package edu.tcu.cs.projectpulse.team.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignStudentsRequest(
        @NotEmpty(message = "At least one student ID is required")
        List<Long> studentIds
) {}
