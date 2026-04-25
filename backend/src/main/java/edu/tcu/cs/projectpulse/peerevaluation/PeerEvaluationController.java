package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvaluationRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvaluationResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.StudentPeerEvaluationReportResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/peer-evaluations")
public class PeerEvaluationController {

    private final PeerEvaluationService peerEvaluationService;

    public PeerEvaluationController(PeerEvaluationService peerEvaluationService) {
        this.peerEvaluationService = peerEvaluationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result submit(@Valid @RequestBody PeerEvaluationRequest request) {
        PeerEvaluationResponse response = peerEvaluationService.submit(request);
        return new Result(true, StatusCode.SUCCESS, "Peer evaluation submitted successfully", response);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody PeerEvaluationRequest request) {
        PeerEvaluationResponse response = peerEvaluationService.update(id, request);
        return new Result(true, StatusCode.SUCCESS, "Peer evaluation updated successfully", response);
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        PeerEvaluationResponse response = peerEvaluationService.findById(id);
        return new Result(true, StatusCode.SUCCESS, "Peer evaluation retrieved successfully", response);
    }

    @GetMapping("/students/{studentId}/report")
    public Result getStudentReport(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        StudentPeerEvaluationReportResponse report = peerEvaluationService.getStudentReport(studentId, weekStart);
        return new Result(true, StatusCode.SUCCESS, "Peer evaluation report generated successfully", report);
    }
}
