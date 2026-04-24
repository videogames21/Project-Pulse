package edu.tcu.cs.projectpulse.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.TestJwtHelper;
import edu.tcu.cs.projectpulse.invitation.InvitationRepository;
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
    @Autowired TestJwtHelper jwtHelper;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        // Remove any test-created users; keep seeded @tcu.edu accounts
        userRepository.findAll().stream()
                .filter(u -> !u.getEmail().endsWith("@tcu.edu"))
                .forEach(u -> userRepository.delete(u));
        invitationRepository.deleteAll();
        // Reset alice's team assignment so profile tests start clean
        userRepository.findByEmail("alice@tcu.edu").ifPresent(u -> {
            u.setTeamId(null);
            userRepository.save(u);
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createInvitationToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andReturn().getResponse().getContentAsString();
        String link = objectMapper.readTree(response).path("data").path("registrationLink").asText();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    /** Registers a fresh student and returns their Bearer token string. */
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

    // ── GET /api/v1/users/me ─────────────────────────────────────────────────

    @Test
    void getMe_studentJwt_returnsOwnData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.name").value("Alice Chen"))
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
        // Team Alpha is in CS4910 which has Dr. Johnson (johnson@tcu.edu) as instructor
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
        // Team Gamma is in CS4911 which has no instructor assigned
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
        var body = Map.of("name", "Updated Name", "email", "testuser@example.com");

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
        var body = Map.of("name", "Test User", "email", "changed@example.com");

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
        var body = Map.of("name", "Test User", "email", "testuser@example.com");

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
        // Try to claim an email that belongs to the seeded admin
        var body = Map.of("name", "Test User", "email", "admin@tcu.edu");

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
        var body = Map.of("name", "", "email", "testuser@example.com");

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
        var body = Map.of("name", "Test User", "email", "not-a-valid-email");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateProfile_withoutJwt_returns403() throws Exception {
        var body = Map.of("name", "Nobody", "email", "nobody@example.com");

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

        // Old password should no longer work
        var oldLogin = Map.of("email", "testuser@example.com", "password", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // New password should work
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
        // All seeded students start unassigned; assign one to verify the filter excludes them
        UserEntity alice = userRepository.findByEmail("alice@tcu.edu").orElseThrow();
        alice.setTeamId(teamIdByName("Team Alpha"));
        userRepository.save(alice);

        String response = mockMvc.perform(get("/api/v1/users?unassigned=true")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].teamId", everyItem(nullValue())))
                .andReturn().getResponse().getContentAsString();

        // Alice is assigned so should not appear
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

    // ── GET /api/v1/users/{id} ────────────────────────────────────────────────

    @Test
    void findById_existingUser_returns200WithCorrectData() throws Exception {
        Long aliceId = userRepository.findByEmail("alice@tcu.edu").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/" + aliceId)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.name").value("Alice Chen"))
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
}
