package edu.tcu.cs.projectpulse.war.dto;

import java.time.LocalDate;
import java.util.List;

public record WARResponse(
        Long id,
        Long studentId,
        LocalDate weekStart,
        List<WARActivityResponse> activities
) {}
