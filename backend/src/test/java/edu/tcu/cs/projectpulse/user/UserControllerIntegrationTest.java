package edu.tcu.cs.projectpulse.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.TestJwtHelper;
import edu.tcu.cs.projectpulse.invitation.InvitationRepository;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UserControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired InvitationRepository invitationRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired SectionRepository sectionRepository;
    @Autowired TestJwtHelper jwtHelper;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();

        invitationRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        sectionRepository.deleteAll();

        // Re-seed base users
        UserEntity admin = new UserEntity();
        admin.setFirstName("Admin"); admin.setLastName("User"); admin.setEmail("admin@tcu.edu");
        admin.setRole(UserRole.ADMIN);
        admin.setPassword("$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2");
        admin.setEnabled(true);
        userRepository.save(admin);

        UserEntity instructor = new UserEntity();
        instructor.setFirstName("Dr."); instructor.setLastName("Johnson"); instructor.setEmail("johnson@tcu.edu");
        instructor.setRole(UserRole.INSTRUCTOR);
        instructor.setPassword("$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2");
        instructor.setEnabled(true);
        instructor = userRepository.save(instructor);

        UserEntity alice = new UserEntity();
        alice.setFirstName("Alice"); alice.setLastName("Chen"); alice.setEmail("alice@tcu.edu");
        alice.setRole(UserRole.STUDENT);
        alice.setPassword("$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2");
        alice.setEnabled(true);
        userRepository.save(alice);

        // Re-seed sections needed for profile tests
        SectionEntity cs4911 = new SectionEntity();
        cs4911.setName("CS4911");
        cs4911.setStartDate(LocalDate.of(2025, 8, 25));
        cs4911.setEndDate(LocalDate.of(2026, 5, 9));
        sectionRepository.save(cs4911);

        SectionEntity cs4910 = new SectionEntity();
        cs4910.setName("CS4910");
        cs4910.setStartDate(LocalDate.of(2024, 8, 26));
        cs4910.setEndDate(LocalDate.of(2025, 5, 10));
        cs4910.setInstructorId(instructor.getId());
        sectionRepository.save(cs4910);

        // Re-seed teams needed for profile tests
        TeamEntity alpha = new TeamEntity();
        alpha.setName("Team Alpha");
        alpha.setDescription("A project about data visualization");
        alpha.setSectionName("CS4910");
        teamRepository.save(alpha);

        TeamEntity gamma = new TeamEntity();
        gamma.setName("Team Gamma");
        gamma.setDescription("AI-powered scheduling tool");
        gamma.setSectionName("CS4911");
        teamRepository.save(gamma);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createInvitationToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andReturn().getResponse().getContentAsString();
        String link = objectMapper.readTree(response).path("data").path("registrationLink").asText();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private String registerStudent(String firstName, String lastName,
                                   String email, String password) throws Exception {
        String invToken = createInvitationToken();
        var body = Map.of("firstName", firstName, "lastName", lastName,
                "email", email, "password", password, "token", invToken);
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("data").path("token").asText();
    }

    private Long teamIdByName(String teamName) {
        return teamRepository.findAll().stream()
                .filter(t -> t.getName().equals(teamName))
                .findFirst().orElseThrow().getId();
    }

    private UserEntity createUser(String firstName, String lastName, String email, UserRole role) {
        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setRole(role);
        return userRepository.save(u);
    }

    // ── GET /api/v1/users/me ─────────────────────────────────────────────────

    @Test
    void getMe_studentJwt_returnsOwnData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Chen"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void getMe_adminJwt_returnsOwnData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@tcu.edu"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void getMe_instructorJwt_returnsOwnData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", jwtHelper.instructorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("johnson@tcu.edu"))
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));
    }

    @Test
    void getMe_withoutJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/users/me/profile ─────────────────────────────────────────

    @Test
    void getMyProfile_studentWithoutTeam_returnsNullTeamFields() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.teamId", nullValue()))
                .andExpect(jsonPath("$.data.teamName", nullValue()))
                .andExpect(jsonPath("$.data.sectionName", nullValue()))
                .andExpect(jsonPath("$.data.instructorName", nullValue()));
    }

    @Test
    void getMyProfile_studentWithTeam_returnsTeamSectionAndInstructorInfo() throws Exception {
        UserEntity alice = userRepository.findByEmail("alice@tcu.edu").orElseThrow();
        alice.setTeamId(teamIdByName("Team Alpha"));
        userRepository.save(alice);

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamName").value("Team Alpha"))
                .andExpect(jsonPath("$.data.sectionName").value("CS4910"))
                .andExpect(jsonPath("$.data.instructorName").value("Dr. Johnson"))
                .andExpect(jsonPath("$.data.instructorEmail").value("johnson@tcu.edu"));
    }

    @Test
    void getMyProfile_studentWithTeamNoInstructor_returnsNullInstructorFields() throws Exception {
        UserEntity alice = userRepository.findByEmail("alice@tcu.edu").orElseThrow();
        alice.setTeamId(teamIdByName("Team Gamma"));
        userRepository.save(alice);

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamName").value("Team Gamma"))
                .andExpect(jsonPath("$.data.sectionName").value("CS4911"))
                .andExpect(jsonPath("$.data.instructorName", nullValue()))
                .andExpect(jsonPath("$.data.instructorEmail", nullValue()));
    }

    @Test
    void getMyProfile_admin_returnsNoTeamInfo() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.teamId", nullValue()))
                .andExpect(jsonPath("$.data.teamName", nullValue()));
    }

    @Test
    void getMyProfile_withoutJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/v1/users/me ─────────────────────────────────────────────────

    @Test
    void updateProfile_validNameChange_returns200WithNewJwt() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("firstName", "Updated", "lastName", "Name", "email", "testuser@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.data.token", not(emptyString())));
    }

    @Test
    void updateProfile_validEmailChange_returns200WithUpdatedEmail() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("firstName", "Test", "lastName", "User", "email", "changed@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("changed@example.com"))
                .andExpect(jsonPath("$.data.token", not(emptyString())));
    }

    @Test
    void updateProfile_sameEmailNoChange_returns200() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("firstName", "Test", "lastName", "User", "email", "testuser@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateProfile_emailTakenByAnotherUser_returns409() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("firstName", "Test", "lastName", "User", "email", "admin@tcu.edu");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateProfile_blankName_returns400() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("firstName", "", "lastName", "User", "email", "testuser@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateProfile_invalidEmailFormat_returns400() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("firstName", "Test", "lastName", "User", "email", "not-a-valid-email");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateProfile_withoutJwt_returns403() throws Exception {
        var body = Map.of("firstName", "Nobody", "lastName", "Test", "email", "nobody@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/v1/users/me/password ─────────────────────────────────────────

    @Test
    void changePassword_correctCurrentPassword_returns200() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("currentPassword", "password123", "newPassword", "newpassword456");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changePassword_newPasswordWorks_oldPasswordFails() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var changeBody = Map.of("currentPassword", "password123", "newPassword", "newpassword456");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeBody)))
                .andExpect(status().isOk());

        var oldLogin = Map.of("email", "testuser@example.com", "password", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        var newLogin = Map.of("email", "testuser@example.com", "password", "newpassword456");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"));
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("currentPassword", "wrongpassword", "newPassword", "newpassword456");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void changePassword_newPasswordTooShort_returns400() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("currentPassword", "password123", "newPassword", "short");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void changePassword_blankCurrentPassword_returns400() throws Exception {
        String jwt = registerStudent("Test", "User", "testuser@example.com", "password123");
        var body = Map.of("currentPassword", "", "newPassword", "newpassword456");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void changePassword_withoutJwt_returns403() throws Exception {
        var body = Map.of("currentPassword", "password123", "newPassword", "newpassword456");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/users ─────────────────────────────────────────────────────

    @Test
    void listStudents_adminRole_returns200WithStudents() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].role").value("STUDENT"));
    }

    @Test
    void listStudents_instructorRole_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", jwtHelper.instructorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listStudents_unassignedFilter_returnsOnlyStudentsWithNoTeam() throws Exception {
        UserEntity alice = userRepository.findByEmail("alice@tcu.edu").orElseThrow();
        alice.setTeamId(teamIdByName("Team Alpha"));
        userRepository.save(alice);

        String response = mockMvc.perform(get("/api/v1/users?unassigned=true")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].teamId", everyItem(nullValue())))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(response).path("data").toString())
                .doesNotContain("alice@tcu.edu");
    }

    @Test
    void listStudents_studentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listStudents_withoutJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/users?role=INSTRUCTOR ────────────────────────────────────

    @Test
    void findInstructors_returnsSeededInstructor() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email").value("johnson@tcu.edu"));
    }

    @Test
    void findInstructors_returnsOnlyInstructors() throws Exception {
        createUser("Extra", "Prof", "extra-prof@example.com", UserRole.INSTRUCTOR);
        createUser("Extra", "Student", "extra-stu@example.com", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_multipleInstructors_returnsAll() throws Exception {
        createUser("Dr.", "Smith", "smith@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_nameFilter_returnsMatchingInstructors() throws Exception {
        createUser("Alice", "Smith", "alice-smith@example.com", UserRole.INSTRUCTOR);
        createUser("Alice", "Teacher", "alice-teacher@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Alice")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_nameFilter_caseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "johnson")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email").value("johnson@tcu.edu"));
    }

    @Test
    void findInstructors_nameFilter_matchesLastName() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Johnson")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lastName").value("Johnson"));
    }

    @Test
    void findInstructors_nameFilter_noMatch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Zzzz")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findInstructors_nameFilter_doesNotReturnStudentsWithSameName() throws Exception {
        createUser("Alice", "Smith", "alice-inst@example.com", UserRole.INSTRUCTOR);
        createUser("Alice", "Smith", "alice-stu@example.com", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Alice")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    // ── GET /api/v1/users/{id} ────────────────────────────────────────────────

    @Test
    void findById_existingStudent_returns200WithCorrectData() throws Exception {
        Long aliceId = userRepository.findByEmail("alice@tcu.edu").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/" + aliceId)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Chen"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void findById_instructorRole_returns200() throws Exception {
        Long aliceId = userRepository.findByEmail("alice@tcu.edu").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/" + aliceId)
                        .header("Authorization", jwtHelper.instructorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(aliceId));
    }

    @Test
    void findById_nonExistentUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void findById_studentRole_returns403() throws Exception {
        Long aliceId = userRepository.findByEmail("alice@tcu.edu").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/" + aliceId)
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findById_withoutJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findById_instructor_returnsInstructorDetail() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice-prof@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Prof"))
                .andExpect(jsonPath("$.data.email").value("alice-prof@example.com"))
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.supervisedTeam").isEmpty());
    }

    @Test
    void findById_instructor_defaultStatusIsActive() throws Exception {
        UserEntity u = createUser("Bob", "Jones", "bob-jones@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void findById_deactivatedInstructor_returnsDeactivatedStatus() throws Exception {
        UserEntity u = new UserEntity();
        u.setFirstName("Carol");
        u.setLastName("Jones");
        u.setEmail("carol-jones@example.com");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(u);

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void findById_instructorAssignedToTeam_returnsSupervisedTeam() throws Exception {
        TeamEntity team = new TeamEntity();
        team.setName("New Team");
        team.setSectionName("CS4910");
        team = teamRepository.save(team);

        UserEntity u = new UserEntity();
        u.setFirstName("Dave");
        u.setLastName("Smith");
        u.setEmail("dave-smith@example.com");
        u.setRole(UserRole.INSTRUCTOR);
        u.setTeamId(team.getId());
        userRepository.save(u);

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam.teamId").isNumber())
                .andExpect(jsonPath("$.data.supervisedTeam.teamName").value("New Team"))
                .andExpect(jsonPath("$.data.supervisedTeam.sectionName").value("CS4910"));
    }

    @Test
    void findById_instructorNotAssigned_supervisedTeamIsNull() throws Exception {
        UserEntity u = createUser("Eve", "Brown", "eve-brown@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());
    }

    // ── PATCH /api/v1/users/{id}/deactivate — UC-23 ──────────────────────────

    @Test
    void deactivateInstructor_activeInstructor_returnsDeactivatedStatus() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice-prof2@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Instructor left the program\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void deactivateInstructor_persistsDeactivatedStatus() throws Exception {
        UserEntity u = createUser("Bob", "Prof", "bob-prof@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No longer teaching\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void deactivateInstructor_removesInstructorFromTeam() throws Exception {
        TeamEntity team = new TeamEntity();
        team.setName("Team Bravo");
        team.setSectionName("CS4910");
        team = teamRepository.save(team);

        UserEntity u = new UserEntity();
        u.setFirstName("Frank");
        u.setLastName("Hill");
        u.setEmail("frank-hill@example.com");
        u.setRole(UserRole.INSTRUCTOR);
        u.setTeamId(team.getId());
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Left university\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());
    }

    @Test
    void deactivateInstructor_emptyReason_returns400() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice-prof3@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateInstructor_whitespaceOnlyReason_returns400() throws Exception {
        UserEntity u = createUser("Alice", "Prof", "alice-prof4@example.com", UserRole.INSTRUCTOR);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateInstructor_nonInstructor_returns400() throws Exception {
        UserEntity student = createUser("Sam", "Student", "sam-student@example.com", UserRole.STUDENT);

        mockMvc.perform(patch("/api/v1/users/" + student.getId() + "/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not an instructor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deactivateInstructor_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/users/9999/deactivate")
                        .header("Authorization", jwtHelper.adminToken())
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
        u.setEmail("carol-jones2@example.com");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/reactivate")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void reactivateInstructor_persistsActiveStatus() throws Exception {
        UserEntity u = new UserEntity();
        u.setFirstName("Dave");
        u.setLastName("Smith");
        u.setEmail("dave-smith2@example.com");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/reactivate")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/" + u.getId())
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void reactivateInstructor_doesNotAutoAssignTeam() throws Exception {
        TeamEntity team = new TeamEntity();
        team.setName("Team Charlie");
        team.setSectionName("CS4911");
        team = teamRepository.save(team);

        UserEntity u = new UserEntity();
        u.setFirstName("Eve");
        u.setLastName("Brown");
        u.setEmail("eve-brown2@example.com");
        u.setRole(UserRole.INSTRUCTOR);
        u.setStatus(UserStatus.DEACTIVATED);
        u = userRepository.save(u);

        mockMvc.perform(patch("/api/v1/users/" + u.getId() + "/reactivate")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedTeam").doesNotExist());
    }

    @Test
    void reactivateInstructor_nonInstructor_returns400() throws Exception {
        UserEntity student = createUser("Sam", "Student", "sam-student2@example.com", UserRole.STUDENT);

        mockMvc.perform(patch("/api/v1/users/" + student.getId() + "/reactivate")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reactivateInstructor_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/users/9999/reactivate")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/v1/users?role=INSTRUCTOR&status=ACTIVE — status filter ───────

    @Test
    void findInstructors_statusActiveFilter_excludesDeactivated() throws Exception {
        createUser("Alice", "Active", "alice-active@example.com", UserRole.INSTRUCTOR);

        UserEntity deactivated = new UserEntity();
        deactivated.setFirstName("Bob");
        deactivated.setLastName("Gone");
        deactivated.setEmail("bob-gone@example.com");
        deactivated.setRole(UserRole.INSTRUCTOR);
        deactivated.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(deactivated);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("status", "ACTIVE")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.email == 'alice-active@example.com')].status",
                        hasItem("ACTIVE")));
    }

    @Test
    void findInstructors_statusActiveFilter_withNameSearch_excludesDeactivated() throws Exception {
        createUser("Zara", "Smith", "zara-smith@example.com", UserRole.INSTRUCTOR);

        UserEntity deactivated = new UserEntity();
        deactivated.setFirstName("Zara");
        deactivated.setLastName("Gone");
        deactivated.setEmail("zara-gone@example.com");
        deactivated.setRole(UserRole.INSTRUCTOR);
        deactivated.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(deactivated);

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "INSTRUCTOR")
                        .param("status", "ACTIVE")
                        .param("name", "Zara")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void findInstructors_noStatusFilter_returnsAllIncludingDeactivated() throws Exception {
        createUser("Yara", "Active", "yara-active@example.com", UserRole.INSTRUCTOR);

        UserEntity deactivated = new UserEntity();
        deactivated.setFirstName("Yara");
        deactivated.setLastName("Gone");
        deactivated.setEmail("yara-gone@example.com");
        deactivated.setRole(UserRole.INSTRUCTOR);
        deactivated.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(deactivated);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }
}
