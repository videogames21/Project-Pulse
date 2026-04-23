package edu.tcu.cs.projectpulse.team;

public class TeamNameConflictException extends RuntimeException {
    public TeamNameConflictException(String name) {
        super("Team name already exists: " + name);
    }
}
