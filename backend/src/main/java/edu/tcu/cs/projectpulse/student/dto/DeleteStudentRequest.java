package edu.tcu.cs.projectpulse.student.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteStudentRequest(
        @NotBlank(message = "Admin password is required")
        String adminPassword
) {}
