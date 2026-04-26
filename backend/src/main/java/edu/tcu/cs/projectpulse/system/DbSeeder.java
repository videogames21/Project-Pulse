package edu.tcu.cs.projectpulse.system;

import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DbSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;

    public DbSeeder(UserRepository userRepository,
                    TeamRepository teamRepository,
                    SectionRepository sectionRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.sectionRepository = sectionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByRole(UserRole.STUDENT).stream()
                .filter(s -> s.getSectionId() == null && s.getTeamId() != null)
                .forEach(student -> teamRepository.findById(student.getTeamId())
                        .flatMap(team -> sectionRepository.findByName(team.getSectionName()))
                        .ifPresent(section -> {
                            student.setSectionId(section.getId());
                            userRepository.save(student);
                        }));
    }
}
