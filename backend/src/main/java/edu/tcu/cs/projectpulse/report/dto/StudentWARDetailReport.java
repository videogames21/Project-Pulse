package edu.tcu.cs.projectpulse.report.dto;

import java.util.List;

public record StudentWARDetailReport(
        Long studentId,
        String firstName,
        String lastName,
        List<WeekWARDetail> weeks
) {}
