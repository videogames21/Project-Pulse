package edu.tcu.cs.projectpulse.auth;

public class InvitationAlreadyUsedException extends RuntimeException {
    public InvitationAlreadyUsedException() {
        super("This registration link has already been used. Please contact your admin for a new link.");
    }
}
