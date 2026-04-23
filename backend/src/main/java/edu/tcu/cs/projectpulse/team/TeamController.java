package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.team.dto.MemberResponse;
import edu.tcu.cs.projectpulse.team.dto.TeamResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
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
        return new Result(true, StatusCode.SUCCESS, "Team retrieved successfully", toResponse(teamService.findById(id)));
    }

    // Returns members of the team that the current user belongs to (UC-28)
    // TODO: replace userId param with JWT principal once auth is implemented
    @GetMapping("/my-team/members")
    public Result myTeamMembers(@RequestParam(defaultValue = "1") Long userId) {
        List<MemberResponse> members = teamService.findMyTeamMembers(userId)
                .stream().map(this::toMemberResponse).toList();
        return new Result(true, StatusCode.SUCCESS, "Team members retrieved", members);
    }

    @GetMapping("/{id}/members")
    public Result members(@PathVariable Long id) {
        List<MemberResponse> members = teamService.findMembers(id)
                .stream().map(this::toMemberResponse).toList();
        return new Result(true, StatusCode.SUCCESS, "Members retrieved", members);
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
