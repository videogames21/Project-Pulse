package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.team.dto.TeamRequest;
import edu.tcu.cs.projectpulse.team.dto.TeamResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        TeamResponse response = toResponse(teamService.findById(id));
        return new Result(true, StatusCode.SUCCESS, "Team retrieved successfully", response);
    }

    @GetMapping
    public Result findAll(
            @RequestParam(required = false) String sectionName,
            @RequestParam(required = false) String teamName) {
        List<TeamResponse> teams = teamService.findAll(sectionName, teamName)
                .stream()
                .map(this::toResponse)
                .toList();
        return new Result(true, StatusCode.SUCCESS, "Teams retrieved successfully", teams);
    }

    @PostMapping
    public Result create(@Valid @RequestBody TeamRequest request) {
        TeamResponse response = toResponse(teamService.create(request));
        return new Result(true, StatusCode.SUCCESS, "Team created successfully", response);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        TeamResponse response = toResponse(teamService.update(id, request));
        return new Result(true, StatusCode.SUCCESS, "Team updated successfully", response);
    }

    private TeamResponse toResponse(TeamEntity entity) {
        return new TeamResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getWebsiteUrl(),
                entity.getSectionName()
        );
    }
}
