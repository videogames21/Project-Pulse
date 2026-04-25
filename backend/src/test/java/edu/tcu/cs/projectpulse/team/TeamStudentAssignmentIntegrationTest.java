package edu.tcu.cs.projectpulse.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithMockUser(roles = "ADMIN")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TeamStudentAssignmentIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    UserRepository userRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        // Delete all users — this class uses @WithMockUser so real DB users are not needed
        // for authentication. @DirtiesContext ensures a fresh context (with data.sql re-run)
        // is created for subsequent test classes.
        userRepository.deleteAll();
        teamRepository.deleteAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long createTeam(String name) {
        TeamEntity t = new TeamEntity();
        t.setName(name);
        t.setSectionName("CS4910");
        return teamRepository.save(t).getId();
    }

    private Long createStudent(String firstName, String lastName, String email) {
        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setRole(UserRole.STUDENT);
        return userRepository.save(u).getId();
    }

    private void assign(Long teamId, List<Long> studentIds) throws Exception {
        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", studentIds))))
                .andExpect(status().isOk());
    }

    // ── GET /api/v1/users ────────────────────────────────────────────────────

    @Test
    void findStudents_noStudents_returns200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findStudents_withStudents_returnsAllStudents() throws Exception {
        createStudent("Alice", "Chen", "alice@tcu.edu");
        createStudent("Bob", "Smith", "bob@tcu.edu");

        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].firstName", containsInAnyOrder("Alice", "Bob")));
    }

    // ── GET /api/v1/users?unassigned=true ────────────────────────────────────

    @Test
    void findUnassignedStudents_allUnassigned_returnsAll() throws Exception {
        createStudent("Alice", "Chen", "alice@tcu.edu");
        createStudent("Bob", "Smith", "bob@tcu.edu");

        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT").param("unassigned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findUnassignedStudents_someAssigned_returnsOnlyUnassigned() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");
        createStudent("Bob", "Smith", "bob@tcu.edu");

        assign(teamId, List.of(aliceId));

        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT").param("unassigned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName").value("Bob"));
    }

    // ── GET /api/v1/users/{id} ───────────────────────────────────────────────

    @Test
    void findUserById_unassignedStudent_returns200WithNullTeamId() throws Exception {
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");

        mockMvc.perform(get("/api/v1/users/" + aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.teamId").isEmpty());
    }

    @Test
    void findUserById_afterAssignment_teamIdIsUpdated() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");

        assign(teamId, List.of(aliceId));

        mockMvc.perform(get("/api/v1/users/" + aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamId").isNumber());
    }

    @Test
    void findUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /api/v1/teams/{id}/students ────────────────────────────────────

    @Test
    void assignStudents_validRequest_returns200() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");
        Long bobId   = createStudent("Bob", "Smith", "bob@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId, bobId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignStudents_studentsAppearInTeamDetailResponse() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");
        Long bobId   = createStudent("Bob", "Smith", "bob@tcu.edu");

        assign(teamId, List.of(aliceId, bobId));

        mockMvc.perform(get("/api/v1/teams/" + teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(2)))
                .andExpect(jsonPath("$.data.students[*].firstName",
                        containsInAnyOrder("Alice", "Bob")));
    }

    @Test
    void assignStudents_teamNotFound_returns404() throws Exception {
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/9999/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignStudents_studentNotFound_returns404() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(9999L)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignStudents_studentAlreadyOnAnotherTeam_returns409() throws Exception {
        Long teamAlphaId = createTeam("Team Alpha");
        Long teamBetaId  = createTeam("Team Beta");
        Long aliceId     = createStudent("Alice", "Chen", "alice@tcu.edu");

        assign(teamAlphaId, List.of(aliceId));

        mockMvc.perform(post("/api/v1/teams/" + teamBetaId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignStudents_emptyStudentIdList_returns400() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── DELETE /api/v1/teams/{id}/students/{studentId} ──────────────────────

    @Test
    void removeStudent_validRequest_returns200() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");

        assign(teamId, List.of(aliceId));

        mockMvc.perform(delete("/api/v1/teams/" + teamId + "/students/" + aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void removeStudent_studentReturnsToUnassignedPool() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");

        assign(teamId, List.of(aliceId));

        mockMvc.perform(delete("/api/v1/teams/" + teamId + "/students/" + aliceId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT").param("unassigned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName").value("Alice"));

        mockMvc.perform(get("/api/v1/teams/" + teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(0)));

        mockMvc.perform(get("/api/v1/users/" + aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamId").isEmpty());
    }

    @Test
    void removeStudent_teamNotFound_returns404() throws Exception {
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");

        mockMvc.perform(delete("/api/v1/teams/9999/students/" + aliceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void removeStudent_studentNotFound_returns404() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(delete("/api/v1/teams/" + teamId + "/students/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void removeStudent_studentNotOnThisTeam_returns409() throws Exception {
        Long teamAlphaId = createTeam("Team Alpha");
        Long teamBetaId  = createTeam("Team Beta");
        Long aliceId     = createStudent("Alice", "Chen", "alice@tcu.edu");

        assign(teamAlphaId, List.of(aliceId));

        mockMvc.perform(delete("/api/v1/teams/" + teamBetaId + "/students/" + aliceId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void removeStudent_userIsNotAStudent_returns400() throws Exception {
        Long teamId = createTeam("Team Alpha");
        UserEntity instructor = new UserEntity();
        instructor.setFirstName("Carol");
        instructor.setLastName("White");
        instructor.setEmail("carol@tcu.edu");
        instructor.setRole(UserRole.INSTRUCTOR);
        instructor.setTeamId(teamId);
        Long instructorId = userRepository.save(instructor).getId();

        mockMvc.perform(delete("/api/v1/teams/" + teamId + "/students/" + instructorId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/v1/teams/{id} (students embedded) ──────────────────────────

    @Test
    void getTeamById_noStudents_returnsEmptyStudentList() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(get("/api/v1/teams/" + teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(0)));
    }

    @Test
    void getTeamById_partialAssignment_onlyShowsAssignedMembers() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long aliceId = createStudent("Alice", "Chen", "alice@tcu.edu");
        Long bobId   = createStudent("Bob", "Smith", "bob@tcu.edu");
        createStudent("Carol", "White", "carol@tcu.edu");

        assign(teamId, List.of(aliceId, bobId));

        mockMvc.perform(get("/api/v1/teams/" + teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(2)))
                .andExpect(jsonPath("$.data.students[*].firstName",
                        containsInAnyOrder("Alice", "Bob")));
    }

    @Test
    void getTeamById_studentsIsolatedBetweenTeams() throws Exception {
        Long teamAlphaId = createTeam("Team Alpha");
        Long teamBetaId  = createTeam("Team Beta");
        Long aliceId     = createStudent("Alice", "Chen", "alice@tcu.edu");
        Long bobId       = createStudent("Bob", "Smith", "bob@tcu.edu");

        assign(teamAlphaId, List.of(aliceId));
        assign(teamBetaId,  List.of(bobId));

        mockMvc.perform(get("/api/v1/teams/" + teamAlphaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(1)))
                .andExpect(jsonPath("$.data.students[0].firstName").value("Alice"));

        mockMvc.perform(get("/api/v1/teams/" + teamBetaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(1)))
                .andExpect(jsonPath("$.data.students[0].firstName").value("Bob"));
    }

    // ── Security enforcement ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "STUDENT")
    void adminEndpoint_withStudentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/invitations"))
                .andExpect(status().isForbidden());
    }
}
