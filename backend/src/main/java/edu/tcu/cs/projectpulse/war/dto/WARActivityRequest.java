package edu.tcu.cs.projectpulse.war.dto;

import edu.tcu.cs.projectpulse.war.ActivityCategory;
import edu.tcu.cs.projectpulse.war.ActivityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WARActivityRequest(
        @NotNull(message = "Category is required")
        ActivityCategory category,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Planned hours are required")
        @Positive(message = "Planned hours must be positive")
        BigDecimal plannedHours,

        @NotNull(message = "Actual hours are required")
        @Positive(message = "Actual hours must be positive")
        BigDecimal actualHours,

        @NotNull(message = "Status is required")
        ActivityStatus status
) {}
