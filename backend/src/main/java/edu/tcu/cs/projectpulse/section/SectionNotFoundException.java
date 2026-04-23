package edu.tcu.cs.projectpulse.section;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(Long id) {
        super("Section not found with id: " + id);
    }

    public SectionNotFoundException(String name) {
        super("Section not found with name: " + name);
    }
}
