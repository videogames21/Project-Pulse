package edu.tcu.cs.projectpulse.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("An account with email " + email + " already exists.");
    }
}
