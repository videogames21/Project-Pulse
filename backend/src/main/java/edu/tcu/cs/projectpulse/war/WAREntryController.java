package edu.tcu.cs.projectpulse.war;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.war.dto.WAREntryRequest;
import edu.tcu.cs.projectpulse.war.dto.WAREntryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/war")
public class WAREntryController {

    private final WAREntryService warEntryService;

    public WAREntryController(WAREntryService warEntryService) {
        this.warEntryService = warEntryService;
    }

    // TODO: replace userId param with JWT principal once auth is implemented
    @GetMapping
    public Result findAll(
            @RequestParam Integer week,
            @RequestParam(defaultValue = "1") Long userId) {
        List<WAREntryResponse> entries = warEntryService.findByStudentAndWeek(userId, week)
                .stream().map(this::toResponse).toList();
        return new Result(true, StatusCode.SUCCESS, "WAR entries retrieved", entries);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result create(
            @Valid @RequestBody WAREntryRequest request,
            @RequestParam(defaultValue = "1") Long userId) {
        return new Result(true, StatusCode.SUCCESS, "Activity added", toResponse(warEntryService.create(userId, request)));
    }

    @PutMapping("/{id}")
    public Result update(
            @PathVariable Long id,
            @Valid @RequestBody WAREntryRequest request,
            @RequestParam(defaultValue = "1") Long userId) {
        return new Result(true, StatusCode.SUCCESS, "Activity updated", toResponse(warEntryService.update(id, userId, request)));
    }

    @DeleteMapping("/{id}")
    public Result delete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {
        warEntryService.delete(id, userId);
        return new Result(true, StatusCode.SUCCESS, "Activity deleted");
    }

    private WAREntryResponse toResponse(WAREntryEntity e) {
        return new WAREntryResponse(
                e.getId(), e.getWeek(), e.getCategory(),
                e.getDescription(), e.getPlannedHours(), e.getActualHours(), e.getStatus()
        );
    }
}
