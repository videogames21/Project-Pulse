package edu.tcu.cs.projectpulse.system;

import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import edu.tcu.cs.projectpulse.user.UserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DbSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;

    public DbSeeder(UserRepository userRepository,
                    TeamRepository teamRepository,
                    SectionRepository sectionRepository,
                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.sectionRepository = sectionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedSections();
        seedTeams();
        linkStudentSections();
    }

    private void seedUsers() {
        String hash = passwordEncoder.encode("password");

        createUserIfAbsent("admin@tcu.edu",    "Admin",  "User",    UserRole.ADMIN,      null);
        createUserIfAbsent("johnson@tcu.edu",  "Dr.",    "Johnson", UserRole.INSTRUCTOR, null);
        createUserIfAbsent("smith@tcu.edu",    "Dr.",    "Smith",   UserRole.INSTRUCTOR, null);
        createUserIfAbsent("jones@tcu.edu",    "Dr.",    "Jones",   UserRole.INSTRUCTOR, null);
        createUserIfAbsent("alice@tcu.edu",    "Alice",  "Chen",    UserRole.STUDENT,    1L);
        createUserIfAbsent("bob@tcu.edu",      "Bob",    "Smith",   UserRole.STUDENT,    1L);
        createUserIfAbsent("carol@tcu.edu",    "Carol",  "White",   UserRole.STUDENT,    1L);
        createUserIfAbsent("dave@tcu.edu",     "Dave",   "Brown",   UserRole.STUDENT,    2L);
        createUserIfAbsent("eve@tcu.edu",      "Eve",    "Davis",   UserRole.STUDENT,    2L);
        createUserIfAbsent("frank@tcu.edu",    "Frank",  "Lee",     UserRole.STUDENT,    2L);
        createUserIfAbsent("grace@tcu.edu",    "Grace",  "Kim",     UserRole.STUDENT,    3L);
        createUserIfAbsent("hank@tcu.edu",     "Hank",   "Jones",   UserRole.STUDENT,    3L);
    }

    private void createUserIfAbsent(String email, String firstName, String lastName,
                                    UserRole role, Long teamId) {
        if (userRepository.findByEmail(email).isPresent()) return;
        UserEntity u = new UserEntity();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setPassword(passwordEncoder.encode("password"));
        u.setEnabled(true);
        u.setTeamId(teamId);
        userRepository.save(u);
    }

    private void seedSections() {
        createSectionIfAbsent("CS4910", LocalDate.of(2024, 8, 26), LocalDate.of(2025, 5, 10));
        createSectionIfAbsent("CS4911", LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5,  9));
    }

    private void createSectionIfAbsent(String name, LocalDate start, LocalDate end) {
        if (sectionRepository.findByName(name).isPresent()) return;
        SectionEntity s = new SectionEntity();
        s.setName(name);
        s.setStartDate(start);
        s.setEndDate(end);
        sectionRepository.save(s);
    }

    private void seedTeams() {
        createTeamIfAbsent("Team Alpha", "A project about data visualization",  "http://teamalpha.com", "CS4910");
        createTeamIfAbsent("Team Beta",  "Mobile app for campus navigation",    "http://teambeta.com",  "CS4910");
        createTeamIfAbsent("Team Gamma", "AI-powered scheduling tool",          null,                   "CS4911");
    }

    private void createTeamIfAbsent(String name, String description, String url, String sectionName) {
        if (teamRepository.existsByName(name)) return;
        TeamEntity t = new TeamEntity();
        t.setName(name);
        t.setDescription(description);
        t.setWebsiteUrl(url);
        t.setSectionName(sectionName);
        teamRepository.save(t);
    }

    private void linkStudentSections() {
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
