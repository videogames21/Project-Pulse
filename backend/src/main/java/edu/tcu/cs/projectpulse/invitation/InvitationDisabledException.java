package edu.tcu.cs.projectpulse.invitation;

public class InvitationDisabledException extends RuntimeException {
    public InvitationDisabledException() {
        super("The invitation link you were using has been disabled. Please contact your admin for assistance.");
    }
}
