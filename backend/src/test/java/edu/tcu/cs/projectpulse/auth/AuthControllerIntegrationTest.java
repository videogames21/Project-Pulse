package edu.tcu.cs.projectpulse.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.TestJwtHelper;
import edu.tcu.cs.projectpulse.invitation.InvitationRepository;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired InvitationRepository invitationRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestJwtHelper jwtHelper;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        // Clean up registrations and invitations between tests; leave seeded users intact
        userRepository.findAll().stream()
                .filter(u -> !u.getEmail().endsWith("@tcu.edu") || u.getEmail().equals("new@student.com"))
                .forEach(u -> userRepository.delete(u));
        invitationRepository.deleteAll();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String createInvitationToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andReturn().getResponse().getContentAsString();
        String link = objectMapper.readTree(response).path("data").path("registrationLink").asText();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private Map<String, String> validRegistrationBody(String token) {
        return Map.of(
                "firstName", "New",
                "lastName", "Student",
                "email", "new@student.com",
                "password", "password123",
                "token", token
        );
    }

    // ── POST /api/v1/auth/register ───────────────────────────────────────────

    @Test
    void register_validTokenAndData_returns200WithJwt() throws Exception {
        String token = createInvitationToken();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationBody(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", not(emptyString())))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.name").value("New Student"));

        // Invitation stays ACTIVE after registration (reusable links)
        mockMvc.perform(get("/api/v1/invitations/" + token))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void register_invalidToken_returns404() throws Exception {
        var body = Map.of("firstName", "A", "lastName", "B",
                "email", "a@b.com", "password", "password123",
                "token", "not-a-real-token");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_sameToken_twoStudents_bothSucceed() throws Exception {
        String token = createInvitationToken();

        // First student registers
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationBody(token))))
                .andExpect(status().isOk());

        // Second student registers with the same ACTIVE token — must also succeed
        var body2 = Map.of("firstName", "Other", "lastName", "Student",
                "email", "other@student.com", "password", "password123",
                "token", token);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", not(emptyString())));
    }

    @Test
    void register_disabledToken_returns410() throws Exception {
        String token = createInvitationToken();

        // Admin disables the invitation
        mockMvc.perform(patch("/api/v1/invitations/" + token + "/disable")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk());

        var body = Map.of("firstName", "New", "lastName", "Student",
                "email", "blocked@student.com", "password", "password123",
                "token", token);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String token1 = createInvitationToken();
        String token2 = createInvitationToken();

        // First registration with email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationBody(token1))))
                .andExpect(status().isOk());

        // Second registration with same email, different token
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationBody(token2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_blankFirstName_returns400() throws Exception {
        String token = createInvitationToken();
        var body = Map.of("firstName", "", "lastName", "Student",
                "email", "x@test.com", "password", "password123", "token", token);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_passwordTooShort_returns400() throws Exception {
        String token = createInvitationToken();
        var body = Map.of("firstName", "New", "lastName", "Student",
                "email", "x@test.com", "password", "short", "token", token);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /api/v1/auth/login ──────────────────────────────────────────────

    @Test
    void login_seededAdmin_returns200WithJwt() throws Exception {
        var body = Map.of("email", "admin@tcu.edu", "password", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", not(emptyString())))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.email").value("admin@tcu.edu"));
    }

    @Test
    void login_seededInstructor_returns200WithJwt() throws Exception {
        var body = Map.of("email", "johnson@tcu.edu", "password", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));
    }

    @Test
    void login_afterRegistration_returns200() throws Exception {
        String token = createInvitationToken();
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationBody(token))));

        var loginBody = Map.of("email", "new@student.com", "password", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        var body = Map.of("email", "admin@tcu.edu", "password", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        var body = Map.of("email", "nobody@tcu.edu", "password", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── JWT enforcement ──────────────────────────────────────────────────────

    @Test
    void adminEndpoint_withoutJwt_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/invitations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withStudentJwt_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/users/me ─────────────────────────────────────────────────

    @Test
    void getMe_studentJwt_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", jwtHelper.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void getMe_adminJwt_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@tcu.edu"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void getMe_withoutJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }
}
