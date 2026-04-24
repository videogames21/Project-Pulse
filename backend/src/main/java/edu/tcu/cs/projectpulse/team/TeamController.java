package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.team.dto.AssignStudentsRequest;
import edu.tcu.cs.projectpulse.team.dto.MemberResponse;
import edu.tcu.cs.projectpulse.team.dto.TeamRequest;
import edu.tcu.cs.projectpulse.team.dto.TeamResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result create(@Valid @RequestBody TeamRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Team created successfully",
                toResponse(teamService.create(request)));
    }

    @GetMapping
    public Result findAll(
            @RequestParam(required = false) String sectionName,
            @RequestParam(required = false) String teamName) {
        List<TeamResponse> teams = teamService.findAll(sectionName, teamName)
                .stream().map(this::toResponse).toList();
        return new Result(true, StatusCode.SUCCESS, "Teams retrieved successfully", teams);
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        return new Result(true, StatusCode.SUCCESS, "Team retrieved successfully",
                toResponse(teamService.findById(id)));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Team updated successfully",
                toResponse(teamService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        teamService.delete(id);
        return new Result(true, StatusCode.SUCCESS, "Team deleted successfully");
    }

    // Returns all members of a specific team
    @GetMapping("/{id}/members")
    public Result members(@PathVariable Long id) {
        List<MemberResponse> members = teamService.findMembers(id)
                .stream().map(this::toMemberResponse).toList();
        return new Result(true, StatusCode.SUCCESS, "Members retrieved", members);
    }

    // Assign students to a team (UC-10)
    @PostMapping("/{id}/students")
    public Result assignStudents(@PathVariable Long id,
                                 @Valid @RequestBody AssignStudentsRequest request) {
        teamService.assignStudents(id, request.studentIds());
        return new Result(true, StatusCode.SUCCESS, "Students assigned successfully");
    }

    // Returns members of the team the current user belongs to (UC-28)
    // TODO: replace userId param with JWT principal once auth is implemented
    @GetMapping("/my-team/members")
    public Result myTeamMembers(@RequestParam(defaultValue = "1") Long userId) {
        List<MemberResponse> members = teamService.findMyTeamMembers(userId)
                .stream().map(this::toMemberResponse).toList();
        return new Result(true, StatusCode.SUCCESS, "Team members retrieved", members);
    }

    // UC-13: Remove a student from a team
    @DeleteMapping("/{teamId}/members/{userId}")
    public Result removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        teamService.removeMember(teamId, userId);
        return new Result(true, StatusCode.SUCCESS, "Member removed from team");
    }

    private TeamResponse toResponse(TeamEntity e) {
        return new TeamResponse(e.getId(), e.getName(), e.getDescription(), e.getWebsiteUrl(), e.getSectionName());
    }

    private MemberResponse toMemberResponse(edu.tcu.cs.projectpulse.user.UserEntity u) {
        return new MemberResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail());
    }
}
