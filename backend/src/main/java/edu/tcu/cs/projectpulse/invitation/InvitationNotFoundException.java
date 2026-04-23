package edu.tcu.cs.projectpulse.invitation;

public class InvitationNotFoundException extends RuntimeException {
    public InvitationNotFoundException(String token) {
        super("Invalid or expired invitation token: " + token);
    }
}
