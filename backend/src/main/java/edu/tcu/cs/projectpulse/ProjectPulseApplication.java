package edu.tcu.cs.projectpulse;

import edu.tcu.cs.projectpulse.auth.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ProjectPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectPulseApplication.class, args);
    }

}
