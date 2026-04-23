package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.team.dto.AssignStudentsRequest;
import edu.tcu.cs.projectpulse.team.dto.TeamResponse;
import edu.tcu.cs.projectpulse.user.dto.UserResponse;
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

    @PostMapping("/{id}/students")
    public Result assignStudents(@PathVariable Long id,
                                 @Valid @RequestBody AssignStudentsRequest request) {
        teamService.assignStudents(id, request.studentIds());
        return new Result(true, StatusCode.SUCCESS, "Students assigned successfully");
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public Result removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        teamService.removeStudent(id, studentId);
        return new Result(true, StatusCode.SUCCESS, "Student removed from team");
    }

    private TeamResponse toResponse(TeamEntity entity) {
        List<UserResponse> students = teamService.findStudentsByTeamId(entity.getId())
                .stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getTeamId()))
                .toList();

        return new TeamResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getWebsiteUrl(),
                entity.getSectionName(),
                students
        );
    }
}
