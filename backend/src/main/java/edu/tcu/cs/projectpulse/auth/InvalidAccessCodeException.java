package edu.tcu.cs.projectpulse.auth;

public class InvalidAccessCodeException extends RuntimeException {
    public InvalidAccessCodeException() {
        super("Invalid access code. Please check with your admin for the correct access code.");
    }
}
