package edu.tcu.cs.projectpulse.activeweek.dto;

import java.time.LocalDate;
import java.util.List;

public record ActiveWeekResponse(
        Long sectionId,
        List<LocalDate> activeWeeks
) {}
