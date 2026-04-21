package edu.tcu.cs.projectpulse.rubric;

public class RubricNameConflictException extends RuntimeException {
    public RubricNameConflictException(String name) {
        super("A rubric with the name \"" + name + "\" already exists");
    }
}
