package edu.tcu.cs.projectpulse.peerevaluation.dto;

public record PeerEvalSubmissionResponse(
        Long id,
        Integer week,
        Long evaluatorId
) {}
