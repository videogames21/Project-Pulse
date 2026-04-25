package edu.tcu.cs.projectpulse.activeweek.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ActiveWeekRequest(
        @NotNull(message = "activeWeekDates must not be null")
        List<LocalDate> activeWeekDates
) {}
