package edu.tcu.cs.projectpulse.peerevaluation;

public class PeerEvaluationNotFoundException extends RuntimeException {

    public PeerEvaluationNotFoundException(Long id) {
        super("Peer evaluation not found with id " + id);
    }
}
