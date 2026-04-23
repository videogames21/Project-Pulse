package edu.tcu.cs.projectpulse.war;

public class WAREntryNotFoundException extends RuntimeException {
    public WAREntryNotFoundException(Long id) {
        super("WAR entry not found with id " + id);
    }
}
