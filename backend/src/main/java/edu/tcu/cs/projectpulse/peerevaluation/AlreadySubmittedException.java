package edu.tcu.cs.projectpulse.peerevaluation;

public class AlreadySubmittedException extends RuntimeException {
    public AlreadySubmittedException(String message) {
        super(message);
    }
}
