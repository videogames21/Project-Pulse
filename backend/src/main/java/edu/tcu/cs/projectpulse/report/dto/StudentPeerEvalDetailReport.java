package edu.tcu.cs.projectpulse.report.dto;

import java.util.List;

public record StudentPeerEvalDetailReport(
        Long studentId,
        String firstName,
        String lastName,
        List<WeekPeerEvalDetail> weeks
) {}
