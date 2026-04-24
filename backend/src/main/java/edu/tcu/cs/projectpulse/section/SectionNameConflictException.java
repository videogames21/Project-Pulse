package edu.tcu.cs.projectpulse.section;

public class SectionNameConflictException extends RuntimeException {
    public SectionNameConflictException(String name) {
        super("A section already exists with name: " + name);
    }
}
