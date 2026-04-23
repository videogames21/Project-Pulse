package edu.tcu.cs.projectpulse.section.dto;

import java.util.List;

public record SectionResponse(
        String sectionName,
        List<String> teamNames
) {}
