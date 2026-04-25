package edu.tcu.cs.projectpulse.war;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.war.dto.TeamWARReportResponse;
import edu.tcu.cs.projectpulse.war.dto.WARActivityRequest;
import edu.tcu.cs.projectpulse.war.dto.WARActivityResponse;
import edu.tcu.cs.projectpulse.war.dto.WARResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/wars")
public class WARController {

    private final WARService warService;

    public WARController(WARService warService) {
        this.warService = warService;
    }

    @GetMapping("/students/{studentId}/weeks/{weekStart}")
    public Result getWAR(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        WARResponse response = warService.getWAR(studentId, weekStart);
        return new Result(true, StatusCode.SUCCESS, "WAR retrieved successfully", response);
    }

    @PostMapping("/students/{studentId}/weeks/{weekStart}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public Result addActivity(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Valid @RequestBody WARActivityRequest request) {
        WARActivityResponse response = warService.addActivity(studentId, weekStart, request);
        return new Result(true, StatusCode.SUCCESS, "Activity added successfully", response);
    }

    @PutMapping("/students/{studentId}/weeks/{weekStart}/activities/{activityId}")
    public Result updateActivity(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable Long activityId,
            @Valid @RequestBody WARActivityRequest request) {
        WARActivityResponse response = warService.updateActivity(studentId, weekStart, activityId, request);
        return new Result(true, StatusCode.SUCCESS, "Activity updated successfully", response);
    }

    @DeleteMapping("/students/{studentId}/weeks/{weekStart}/activities/{activityId}")
    public Result deleteActivity(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable Long activityId) {
        warService.deleteActivity(studentId, weekStart, activityId);
        return new Result(true, StatusCode.SUCCESS, "Activity deleted successfully");
    }

    @GetMapping("/teams/{teamId}/report")
    public Result getTeamReport(
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        TeamWARReportResponse report = warService.getTeamReport(teamId, weekStart);
        return new Result(true, StatusCode.SUCCESS, "Team WAR report generated successfully", report);
    }
}
