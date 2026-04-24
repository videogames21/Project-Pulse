package edu.tcu.cs.projectpulse;

import edu.tcu.cs.projectpulse.auth.JwtService;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestJwtHelper {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    public String adminToken() {
        return "Bearer " + jwtService.generateToken(
                userRepository.findByEmail("admin@tcu.edu").orElseThrow());
    }

    public String instructorToken() {
        return "Bearer " + jwtService.generateToken(
                userRepository.findByEmail("johnson@tcu.edu").orElseThrow());
    }

    public String studentToken() {
        return "Bearer " + jwtService.generateToken(
                userRepository.findByEmail("alice@tcu.edu").orElseThrow());
    }
}
