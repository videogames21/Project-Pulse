package edu.tcu.cs.projectpulse.section.dto;

import java.time.LocalDate;
import java.util.List;

public record SectionResponse(
        Long id,
        String sectionName,
        LocalDate startDate,
        LocalDate endDate,
        List<String> teamNames,
        Long instructorId
) {}
