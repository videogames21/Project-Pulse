package edu.tcu.cs.projectpulse.war;

import java.time.LocalDate;

public class WARNotFoundException extends RuntimeException {

    public WARNotFoundException(Long studentId, LocalDate weekStart) {
        super("WAR not found for student " + studentId + " and week " + weekStart);
    }

    public WARNotFoundException(Long activityId) {
        super("WAR activity not found with id " + activityId);
    }
}
