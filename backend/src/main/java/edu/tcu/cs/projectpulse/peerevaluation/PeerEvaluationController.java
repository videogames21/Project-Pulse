package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.peerevaluation.dto.SubmitPeerEvalRequest;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/peer-evaluations")
public class PeerEvaluationController {

    private final PeerEvaluationService peerEvaluationService;

    public PeerEvaluationController(PeerEvaluationService peerEvaluationService) {
        this.peerEvaluationService = peerEvaluationService;
    }

    // Returns existing submission or null data if not yet submitted (BR-3 check)
    // TODO: replace userId param with JWT principal once auth is implemented
    @GetMapping("/my-submission")
    public Result mySubmission(
            @RequestParam Integer week,
            @RequestParam(defaultValue = "1") Long userId) {
        return new Result(true, StatusCode.SUCCESS, "Submission status retrieved",
                peerEvaluationService.findMySubmission(userId, week).orElse(null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result submit(
            @Valid @RequestBody SubmitPeerEvalRequest request,
            @RequestParam(defaultValue = "1") Long userId) {
        peerEvaluationService.submit(userId, request);
        return new Result(true, StatusCode.SUCCESS, "Peer evaluation submitted successfully");
    }
}
