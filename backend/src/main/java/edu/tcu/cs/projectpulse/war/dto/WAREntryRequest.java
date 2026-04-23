package edu.tcu.cs.projectpulse.war.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WAREntryRequest(
        @NotNull(message = "Week is required")
        Integer week,

        @NotBlank(message = "Category is required")
        String category,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Planned hours are required")
        @DecimalMin(value = "0.0", message = "Planned hours cannot be negative")
        BigDecimal plannedHours,

        @NotNull(message = "Actual hours are required")
        @DecimalMin(value = "0.0", message = "Actual hours cannot be negative")
        BigDecimal actualHours,

        @NotBlank(message = "Status is required")
        String status
) {}
