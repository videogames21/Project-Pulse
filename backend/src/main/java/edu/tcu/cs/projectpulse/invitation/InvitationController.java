package edu.tcu.cs.projectpulse.invitation;

import edu.tcu.cs.projectpulse.invitation.dto.InvitationResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
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

    @GetMapping("/{token}")
    public Result findByToken(@PathVariable String token) {
        InvitationResponse response = service.findByToken(token);
        return new Result(true, StatusCode.SUCCESS, "Found", response);
    }

    @PatchMapping("/{token}/disable")
    public Result disable(@PathVariable String token) {
        InvitationResponse response = service.disableInvitation(token);
        return new Result(true, StatusCode.SUCCESS, "Invitation disabled.", response);
    }

    @PatchMapping("/{token}/enable")
    public Result enable(@PathVariable String token) {
        InvitationResponse response = service.enableInvitation(token);
        return new Result(true, StatusCode.SUCCESS, "Invitation enabled.", response);
    }

    @DeleteMapping("/{token}")
    public Result delete(@PathVariable String token) {
        service.deleteInvitation(token);
        return new Result(true, StatusCode.SUCCESS, "Invitation deleted.", null);
    }
}
