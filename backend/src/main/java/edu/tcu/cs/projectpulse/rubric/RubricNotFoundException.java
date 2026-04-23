package edu.tcu.cs.projectpulse.rubric;

public class RubricNotFoundException extends RuntimeException {
    public RubricNotFoundException(Long id) {
        super("Rubric not found with id " + id);
    }

    public RubricNotFoundException(String message) {
        super(message);
    }
}
