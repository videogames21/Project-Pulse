package edu.tcu.cs.projectpulse.section;

import edu.tcu.cs.projectpulse.section.dto.SectionDetailResponse;
import edu.tcu.cs.projectpulse.section.dto.SectionRequest;
import edu.tcu.cs.projectpulse.section.dto.SectionResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result create(@Valid @RequestBody SectionRequest request) {
        SectionDetailResponse section = sectionService.create(request);
        return new Result(true, StatusCode.SUCCESS, "Section created successfully", section);
    }

    @GetMapping
    public Result findAll(@RequestParam(required = false) String name) {
        List<SectionResponse> sections = sectionService.findAll(name);
        return new Result(true, StatusCode.SUCCESS, "Sections retrieved successfully", sections);
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        SectionDetailResponse section = sectionService.findSectionById(id);
        return new Result(true, StatusCode.SUCCESS, "Section retrieved successfully", section);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        SectionDetailResponse section = sectionService.update(id, request);
        return new Result(true, StatusCode.SUCCESS, "Section updated successfully", section);
    }
}
