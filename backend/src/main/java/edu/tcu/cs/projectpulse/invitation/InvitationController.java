package edu.tcu.cs.projectpulse.invitation;

import edu.tcu.cs.projectpulse.invitation.dto.InvitationResponse;
import edu.tcu.cs.projectpulse.invitation.dto.InviteInstructorsRequest;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService service;

    public InvitationController(InvitationService service) {
        this.service = service;
    }

    @PostMapping
    public Result generate() {
        InvitationResponse response = service.generateInvitation();
        return new Result(true, StatusCode.SUCCESS, "Invitation link generated.", response);
    }

    @GetMapping
    public Result findAll() {
        List<InvitationResponse> invitations = service.findAll();
        return new Result(true, StatusCode.SUCCESS, "Find All Success", invitations);
    }

    @PostMapping("/instructors")
    @ResponseStatus(HttpStatus.CREATED)
    public Result inviteInstructors(@Valid @RequestBody InviteInstructorsRequest request) {
        List<InvitationResponse> responses = service.inviteInstructors(request.emails());
        return new Result(true, StatusCode.SUCCESS, responses.size() + " invitation(s) sent.", responses);
    }

    @GetMapping("/{token}")
    public Result findByToken(@PathVariable String token) {
        InvitationResponse response = service.findByToken(token);
        return new Result(true, StatusCode.SUCCESS, "Found", response);
    }
}
