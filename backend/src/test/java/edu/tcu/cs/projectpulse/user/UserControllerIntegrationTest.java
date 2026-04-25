package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@SpringBootTest
class UserControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        userRepository.deleteAll();
        teamRepository.deleteAll();
    }

    private UserEntity createUser(String firstName, String lastName, String email, UserRole role) {
        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setRole(role);
        return userRepository.save(u);
    }

    // ── GET /api/v1/users?role=INSTRUCTOR ────────────────────────────────────

    @Test
    void findInstructors_noInstructors_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findInstructors_returnsOnlyInstructors() throws Exception {
        createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Bob", "Student", "bob@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName").value("Alice"))
                .andExpect(jsonPath("$.data[0].lastName").value("Prof"))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    @Test
    void findInstructors_multipleInstructors_returnsAll() throws Exception {
        createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Charlie", "Prof", "charlie@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_responseIncludesIdFirstNameLastNameEmailRole() throws Exception {
        createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].firstName").value("Alice"))
                .andExpect(jsonPath("$.data[0].lastName").value("Prof"))
                .andExpect(jsonPath("$.data[0].email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    // ── GET /api/v1/users?role=INSTRUCTOR&name= ───────────────────────────────

    @Test
    void findInstructors_nameFilter_returnsMatchingInstructors() throws Exception {
        createUser("Alice", "Smith", "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Bob", "Johnson", "bob@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Alice", "Teacher", "alice2@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_nameFilter_caseInsensitive() throws Exception {
        createUser("Alice", "Smith", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName").value("Alice"));
    }

    @Test
    void findInstructors_nameFilter_matchesLastName() throws Exception {
        createUser("Bob", "Johnson", "bob@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Johnson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lastName").value("Johnson"));
    }

    @Test
    void findInstructors_nameFilter_noMatch_returnsEmpty() throws Exception {
        createUser("Alice", "Smith", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findInstructors_nameFilter_doesNotReturnStudentsWithSameName() throws Exception {
        createUser("Alice", "Smith", "alice-inst@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Alice", "Smith", "alice-student@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    @Test
    void findInstructors_emptyNameParam_returnsAllInstructors() throws Exception {
        createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Bob", "Prof", "bob@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_nameFilter_matchesFullName_acrossFirstAndLastName() throws Exception {
        createUser("Dr.", "Smith", "smith@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Alice", "Johnson", "alice@tcu.edu", UserRole.INSTRUCTOR);

        // "Dr. S" spans firstName + lastName — must match Dr. Smith
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Dr. S"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lastName").value("Smith"));

        // Full name search
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Dr. Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lastName").value("Smith"));
    }

    // ── Existing student behaviour not broken ─────────────────────────────────

    @Test
    void findStudents_roleStudentParam_returnsStudentsOnly() throws Exception {
        createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Bob", "Student", "bob@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName").value("Bob"));
    }

    // ── GET /api/v1/users/{id} — UC-22 instructor detail ─────────────────────

    @Test
    void findById_instructor_returnsInstructorDetail() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Prof"))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.supervisedTeam").isEmpty());
    }

    @Test
    void findById_instructor_defaultStatusIsActive() throws Exception {
        UserEntity u = createUser("Bob", "Jones", "bob@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void findById_deactivatedInstructor_returnsDeactivatedStatus() throws Exception {
        UserEntity u = new UserEntity();
        u.setFirstName("Carol");
        u.setLastName("Jones");
        u.setEmail("carol@tcu.edu");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(u);

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void findById_instructorAssignedToTeam_returnsSupervisedTeam() throws Exception {
        TeamEntity team = new TeamEntity();
        team.setName("Team Alpha");
        team.setSectionName("2025-2026");
        team = teamRepository.save(team);

        UserEntity u = new UserEntity();
        u.setFirstName("Dave");
        u.setLastName("Smith");
        u.setEmail("dave@tcu.edu");
        u.setRole(UserRole.INSTRUCTOR);
        u.setTeamId(team.getId());
        userRepository.save(u);

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam.teamId").isNumber())
                .andExpect(jsonPath("$.data.supervisedTeam.teamName").value("Team Alpha"))
                .andExpect(jsonPath("$.data.supervisedTeam.sectionName").value("2025-2026"));
    }

    @Test
    void findById_instructorNotAssigned_supervisedTeamIsNull() throws Exception {
        UserEntity u = createUser("Eve", "Brown", "eve@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PATCH /api/v1/users/{id}/deactivate — UC-23 ──────────────────────────

    @Test
    void deactivateInstructor_activeInstructor_returnsDeactivatedStatus() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Instructor left the program\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void deactivateInstructor_persistsDeactivatedStatus() throws Exception {
        UserEntity u = createUser("Bob", "Prof", "bob@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No longer teaching\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void deactivateInstructor_removesInstructorFromTeam() throws Exception {
        TeamEntity team = new TeamEntity();
        team.setName("Team Bravo");
        team.setSectionName("2025-2026");
        team = teamRepository.save(team);

        UserEntity u = new UserEntity();
        u.setFirstName("Frank");
        u.setLastName("Hill");
        u.setEmail("frank@tcu.edu");
        u.setRole(UserRole.INSTRUCTOR);
        u.setTeamId(team.getId());
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Left university\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());
    }

    @Test
    void deactivateInstructor_emptyReason_returns400() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice2@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateInstructor_whitespaceOnlyReason_returns400() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice3@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateInstructor_nonInstructor_returns400() throws Exception {
        UserEntity student = createUser("Sam", "Student", "sam@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(patch("/api/v1/users/" + student.getId() + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not an instructor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deactivateInstructor_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/users/9999/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Test reason\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PATCH /api/v1/users/{id}/reactivate — UC-24 ──────────────────────────

    @Test
    void reactivateInstructor_deactivatedInstructor_returnsActiveStatus() throws Exception {
        UserEntity u = new UserEntity();
        u.setFirstName("Carol");
        u.setLastName("Jones");
        u.setEmail("carol@tcu.edu");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void reactivateInstructor_persistsActiveStatus() throws Exception {
        UserEntity u = new UserEntity();
        u.setFirstName("Dave");
        u.setLastName("Smith");
        u.setEmail("dave@tcu.edu");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/reactivate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/" + u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void reactivateInstructor_doesNotAutoAssignTeam() throws Exception {
        TeamEntity team = new TeamEntity();
        team.setName("Team Charlie");
        team.setSectionName("2025-2026");
        team = teamRepository.save(team);

        UserEntity u = new UserEntity();
        u.setFirstName("Eve");
        u.setLastName("Brown");
        u.setEmail("eve@tcu.edu");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());
    }

    @Test
    void reactivateInstructor_nonInstructor_returns400() throws Exception {
        UserEntity student = createUser("Sam", "Student", "sam2@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(patch("/api/v1/users/" + student.getId() + "/reactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reactivateInstructor_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/users/9999/reactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/v1/users?role=INSTRUCTOR&status=ACTIVE — status filter ───────

    @Test
    void findInstructors_statusActiveFilter_excludesDeactivated() throws Exception {
        createUser("Alice", "Active", "alice-active@tcu.edu", UserRole.INSTRUCTOR);

        UserEntity deactivated = new UserEntity();
        deactivated.setFirstName("Bob");
        deactivated.setLastName("Gone");
        deactivated.setEmail("bob-gone@tcu.edu");
        deactivated.setRole(UserRole.INSTRUCTOR);
        deactivated.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(deactivated);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName").value("Alice"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void findInstructors_statusActiveFilter_withNameSearch_excludesDeactivated() throws Exception {
        createUser("Carol", "Smith", "carol@tcu.edu", UserRole.INSTRUCTOR);

        UserEntity deactivated = new UserEntity();
        deactivated.setFirstName("Carol");
        deactivated.setLastName("Gone");
        deactivated.setEmail("carol-gone@tcu.edu");
        deactivated.setRole(UserRole.INSTRUCTOR);
        deactivated.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(deactivated);

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "INSTRUCTOR")
                        .param("status", "ACTIVE")
                        .param("name", "Carol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void findInstructors_noStatusFilter_returnsAllIncludingDeactivated() throws Exception {
        createUser("Alice", "Active", "alice-a@tcu.edu", UserRole.INSTRUCTOR);

        UserEntity deactivated = new UserEntity();
        deactivated.setFirstName("Bob");
        deactivated.setLastName("Gone");
        deactivated.setEmail("bob-g@tcu.edu");
        deactivated.setRole(UserRole.INSTRUCTOR);
        deactivated.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(deactivated);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
