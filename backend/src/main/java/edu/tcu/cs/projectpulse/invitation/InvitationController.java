package edu.tcu.cs.projectpulse.invitation;

import edu.tcu.cs.projectpulse.invitation.dto.InvitationResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@CrossOrigin(origins = "http://localhost:3000")
public class InvitationController {

    private final InvitationService service;

    public InvitationController(InvitationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Result> generate() {
        InvitationResponse response = service.generateInvitation();
        return ResponseEntity.ok(new Result(true, response, "Invitation link generated.", null));
    }

    @GetMapping
    public ResponseEntity<Result> findAll() {
        List<InvitationResponse> invitations = service.findAll();
        return ResponseEntity.ok(new Result(true, invitations, "Find All Success", null));
    }

    @GetMapping("/{token}")
    public ResponseEntity<Result> findByToken(@PathVariable String token) {
        try {
            InvitationResponse response = service.findByToken(token);
            return ResponseEntity.ok(new Result(true, response, "Found", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(StatusCode.NOT_FOUND)
                    .body(new Result(false, null, e.getMessage(), e.getMessage()));
        }
    }
}
