package edu.tcu.cs.projectpulse.activeweek;

import edu.tcu.cs.projectpulse.activeweek.dto.ActiveWeekRequest;
import edu.tcu.cs.projectpulse.activeweek.dto.ActiveWeekResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sections/{sectionId}/active-weeks")
public class ActiveWeekController {

    private final ActiveWeekService activeWeekService;

    public ActiveWeekController(ActiveWeekService activeWeekService) {
        this.activeWeekService = activeWeekService;
    }

    @GetMapping
    public Result getActiveWeeks(@PathVariable Long sectionId) {
        ActiveWeekResponse response = activeWeekService.getActiveWeeks(sectionId);
        return new Result(true, StatusCode.SUCCESS, "Active weeks retrieved successfully", response);
    }

    @PutMapping
    public Result saveActiveWeeks(@PathVariable Long sectionId,
                                  @Valid @RequestBody ActiveWeekRequest request) {
        ActiveWeekResponse response = activeWeekService.saveActiveWeeks(sectionId, request);
        return new Result(true, StatusCode.SUCCESS, "Active weeks saved successfully", response);
    }
}
