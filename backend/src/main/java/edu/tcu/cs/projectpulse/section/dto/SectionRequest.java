package edu.tcu.cs.projectpulse.section.dto;

import edu.tcu.cs.projectpulse.rubric.dto.CriterionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record SectionRequest(
        @NotBlank(message = "Section name is required")
        String name,

        LocalDate startDate,

        LocalDate endDate,

        Long rubricId,

        @Valid
        List<CriterionRequest> criteria,

        Long instructorId
) {}
