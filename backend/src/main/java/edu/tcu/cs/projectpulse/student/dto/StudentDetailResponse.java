package edu.tcu.cs.projectpulse.student.dto;

import java.util.List;

public record StudentDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Long teamId,
        String teamName,
        Long sectionId,
        String sectionName,
        List<StudentWAREntry> wars,
        List<StudentPeerEvalEntry> peerEvaluationsReceived
) {}
