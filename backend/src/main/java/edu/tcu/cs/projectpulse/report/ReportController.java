package edu.tcu.cs.projectpulse.report;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // UC-29: Student views own peer eval report for a week
    // TODO: replace userId param with JWT principal once auth is implemented
    @GetMapping("/peer-eval/student/me")
    public Result myPeerEvalReport(
            @RequestParam Integer week,
            @RequestParam(defaultValue = "1") Long userId) {
        return new Result(true, StatusCode.SUCCESS, "Report retrieved",
                reportService.getMyPeerEvalReport(userId, week));
    }

    // UC-31: Instructor views peer eval report for all students in section
    @GetMapping("/peer-eval/section")
    public Result sectionPeerEvalReport(@RequestParam Integer week) {
        return new Result(true, StatusCode.SUCCESS, "Section report retrieved",
                reportService.getSectionPeerEvalReport(week));
    }

    // UC-32: Instructor/Student views WAR report for a team
    @GetMapping("/war/team/{teamId}")
    public Result teamWARReport(
            @PathVariable Long teamId,
            @RequestParam Integer week) {
        return new Result(true, StatusCode.SUCCESS, "Team WAR report retrieved",
                reportService.getTeamWARReport(teamId, week));
    }

    // UC-33: Instructor views full peer eval detail for a student
    @GetMapping("/peer-eval/student/{id}")
    public Result studentPeerEvalDetail(
            @PathVariable Long id,
            @RequestParam Integer startWeek,
            @RequestParam Integer endWeek) {
        return new Result(true, StatusCode.SUCCESS, "Student peer eval report retrieved",
                reportService.getStudentPeerEvalDetail(id, startWeek, endWeek));
    }

    // UC-34: Instructor views WAR detail for a student
    @GetMapping("/war/student/{id}")
    public Result studentWARDetail(
            @PathVariable Long id,
            @RequestParam Integer startWeek,
            @RequestParam Integer endWeek) {
        return new Result(true, StatusCode.SUCCESS, "Student WAR report retrieved",
                reportService.getStudentWARDetail(id, startWeek, endWeek));
    }
}
