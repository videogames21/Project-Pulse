package edu.tcu.cs.projectpulse.team.dto;

import edu.tcu.cs.projectpulse.user.dto.UserResponse;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String description,
        String websiteUrl,
        String sectionName,
        List<UserResponse> students,
        List<UserResponse> instructors
) {}
