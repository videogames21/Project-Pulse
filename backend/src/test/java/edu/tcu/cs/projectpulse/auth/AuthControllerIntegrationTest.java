package edu.tcu.cs.projectpulse.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.TestJwtHelper;
import edu.tcu.cs.projectpulse.invitation.InvitationRepository;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import edu.tcu.cs.projectpulse.invitation.InvitationEntity;
import edu.tcu.cs.projectpulse.invitation.InvitationStatus;

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
    @Autowired SectionRepository sectionRepository;
    @Autowired TestJwtHelper jwtHelper;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;
    Long testSectionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();

        invitationRepository.deleteAll();
        userRepository.deleteAll();
        sectionRepository.deleteAll();

        SectionEntity testSection = new SectionEntity();
        testSection.setName("TEST_SECTION");
        testSectionId = sectionRepository.save(testSection).getId();

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
        userRepository.save(instructor);

        UserEntity alice = new UserEntity();
        alice.setFirstName("Alice"); alice.setLastName("Chen"); alice.setEmail("alice@tcu.edu");
        alice.setRole(UserRole.STUDENT);
        alice.setPassword("$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2");
        alice.setEnabled(true);
        userRepository.save(alice);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String createInvitationToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionId\":" + testSectionId + "}"))
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

    // ── Instructor Registration ──────────────────────────────────────────────

    @Test
    void register_instructorLink_validAccessCode_returns200WithInstructorRole() throws Exception {
        JsonNode invData = createInstructorInvitationData();
        String token     = invData.path("token").asText();
        String code      = invData.path("accessCode").asText();

        var body = Map.of("firstName", "New", "lastName", "Instructor",
                "email", "newinstructor@tcu.edu", "password", "password123",
                "token", token, "accessCode", code);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));

        // Invitation should now be ACCEPTED (single-use)
        mockMvc.perform(get("/api/v1/invitations/" + token))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void register_instructorLink_invalidAccessCode_returns400() throws Exception {
        JsonNode invData = createInstructorInvitationData();
        String token     = invData.path("token").asText();

        var body = Map.of("firstName", "New", "lastName", "Instructor",
                "email", "badinstructor@tcu.edu", "password", "password123",
                "token", token, "accessCode", "WRONG1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_instructorLink_alreadyAccepted_returns409() throws Exception {
        JsonNode invData = createInstructorInvitationData();
        String token     = invData.path("token").asText();
        String code      = invData.path("accessCode").asText();

        // First instructor registers
        var body1 = Map.of("firstName", "First", "lastName", "Instructor",
                "email", "first.instructor@tcu.edu", "password", "password123",
                "token", token, "accessCode", code);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk());

        // Second attempt on same (now ACCEPTED) link must fail with 409
        var body2 = Map.of("firstName", "Second", "lastName", "Instructor",
                "email", "second.instructor@tcu.edu", "password", "password123",
                "token", token, "accessCode", code);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private JsonNode createInstructorInvitationData() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations/instructor")
                        .header("Authorization", jwtHelper.adminToken()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }
}
