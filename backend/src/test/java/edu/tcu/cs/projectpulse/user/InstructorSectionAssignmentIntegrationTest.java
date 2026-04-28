package edu.tcu.cs.projectpulse.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.TestJwtHelper;
import edu.tcu.cs.projectpulse.invitation.InvitationRepository;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class InstructorSectionAssignmentIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired SectionRepository sectionRepository;
    @Autowired InvitationRepository invitationRepository;
    @Autowired TestJwtHelper jwtHelper;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    Long instrAId;
    Long instrBId;
    Long studentId;
    Long cs4910Id;
    Long cs4911Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
        sectionRepository.deleteAll();

        UserEntity admin = new UserEntity();
        admin.setFirstName("Admin"); admin.setLastName("User"); admin.setEmail("admin@tcu.edu");
        admin.setRole(UserRole.ADMIN); admin.setStatus(UserStatus.ACTIVE);
        admin.setPassword("$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2");
        admin.setEnabled(true);
        userRepository.save(admin);

        instrAId = createInstructor("Dr. Alpha", "alpha@tcu.edu");
        instrBId = createInstructor("Dr. Beta", "beta@tcu.edu");
        studentId = createStudent("Alice Chen", "alice@tcu.edu");
        cs4910Id = createSection("CS4910");
        cs4911Id = createSection("CS4911");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long createInstructor(String fullName, String email) {
        String[] parts = fullName.split(" ", 2);
        UserEntity u = new UserEntity();
        u.setFirstName(parts[0]); u.setLastName(parts.length > 1 ? parts[1] : "");
        u.setEmail(email); u.setRole(UserRole.INSTRUCTOR); u.setStatus(UserStatus.ACTIVE);
        return userRepository.save(u).getId();
    }

    private Long createStudent(String fullName, String email) {
        String[] parts = fullName.split(" ", 2);
        UserEntity u = new UserEntity();
        u.setFirstName(parts[0]); u.setLastName(parts.length > 1 ? parts[1] : "");
        u.setEmail(email); u.setRole(UserRole.STUDENT); u.setStatus(UserStatus.ACTIVE);
        return userRepository.save(u).getId();
    }

    private Long createSection(String name) {
        SectionEntity s = new SectionEntity();
        s.setName(name);
        return sectionRepository.save(s).getId();
    }

    // ── Change 2: Instructor invite with section ──────────────────────────────

    @Test
    void generateInstructorLink_withSectionId_returns201WithSectionName() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructor")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", cs4910Id))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").value("CS4910"))
                .andExpect(jsonPath("$.data.sectionId").value(cs4910Id))
                .andExpect(jsonPath("$.data.accessCode", hasLength(6)))
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));
    }

    @Test
    void generateInstructorLink_withInvalidSectionId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructor")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", 99999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generateInstructorLink_noBody_returns201WithNullSectionName() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructor")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").doesNotExist())
                .andExpect(jsonPath("$.data.accessCode", hasLength(6)));
    }

    @Test
    void register_viaInstructorLinkWithSection_autoAssignsSectionId() throws Exception {
        // Generate instructor link scoped to CS4910
        String invRes = mockMvc.perform(post("/api/v1/invitations/instructor")
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", cs4910Id))))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(invRes).path("data").path("token").asText();
        String code  = objectMapper.readTree(invRes).path("data").path("accessCode").asText();

        // Register new instructor via that link
        var regBody = Map.of(
                "firstName", "New", "lastName", "Instructor",
                "email", "newinstructor@tcu.edu",
                "password", "password123",
                "token", token,
                "accessCode", code
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regBody)))
                .andExpect(status().isOk());

        // Verify new instructor has supervisedSectionName == "CS4910"
        mockMvc.perform(get("/api/v1/users")
                        .param("role", "INSTRUCTOR")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email == 'newinstructor@tcu.edu')].supervisedSectionName",
                        hasItem("CS4910")));
    }

    // ── Change 3: Multiple instructors per section ────────────────────────────

    @Test
    void updateSection_withTwoInstructorIds_assignsBothToSection() throws Exception {
        var body = Map.of("name", "CS4910", "instructorIds", List.of(instrAId, instrBId));

        mockMvc.perform(put("/api/v1/sections/{id}", cs4910Id)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors", hasSize(2)));

        // Both instructors should now have supervisedSectionName = CS4910
        mockMvc.perform(get("/api/v1/users/{id}", instrAId)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedSectionName").value("CS4910"));

        mockMvc.perform(get("/api/v1/users/{id}", instrBId)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedSectionName").value("CS4910"));
    }

    @Test
    void updateSection_removeOneInstructor_clearsSectionIdForRemoved() throws Exception {
        // Assign both
        var bodyBoth = Map.of("name", "CS4910", "instructorIds", List.of(instrAId, instrBId));
        mockMvc.perform(put("/api/v1/sections/{id}", cs4910Id)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyBoth)))
                .andExpect(status().isOk());

        // Remove instrB
        var bodyOne = Map.of("name", "CS4910", "instructorIds", List.of(instrAId));
        mockMvc.perform(put("/api/v1/sections/{id}", cs4910Id)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyOne)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors", hasSize(1)));

        // instrB should have no section now
        mockMvc.perform(get("/api/v1/users/{id}", instrBId)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedSectionName").doesNotExist());
    }

    @Test
    void updateSection_emptyInstructorIds_unassignsAll() throws Exception {
        // First assign instrA
        var bodyOne = Map.of("name", "CS4910", "instructorIds", List.of(instrAId));
        mockMvc.perform(put("/api/v1/sections/{id}", cs4910Id)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyOne)))
                .andExpect(status().isOk());

        // Now clear all
        var bodyEmpty = Map.of("name", "CS4910", "instructorIds", List.of());
        mockMvc.perform(put("/api/v1/sections/{id}", cs4910Id)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEmpty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors", hasSize(0)));
    }

    @Test
    void getSection_noInstructors_returnsEmptyInstructorsList() throws Exception {
        mockMvc.perform(get("/api/v1/sections/{id}", cs4910Id)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors").isArray())
                .andExpect(jsonPath("$.data.instructors", hasSize(0)));
    }

    // ── Change 4: Assign section from instructor detail ───────────────────────

    @Test
    void assignSection_validSectionId_returns200AndUpdatesSection() throws Exception {
        var body = Map.of("sectionId", cs4910Id);

        mockMvc.perform(patch("/api/v1/users/{id}/section", instrAId)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.supervisedSectionName").value("CS4910"))
                .andExpect(jsonPath("$.data.supervisedSectionId").value(cs4910Id));
    }

    @Test
    void assignSection_nullSectionId_unassignsInstructor() throws Exception {
        // First assign
        userRepository.findById(instrAId).ifPresent(u -> {
            u.setSectionId(cs4910Id);
            userRepository.save(u);
        });

        // Now unassign
        String body = "{\"sectionId\": null}";
        mockMvc.perform(patch("/api/v1/users/{id}/section", instrAId)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisedSectionName").doesNotExist())
                .andExpect(jsonPath("$.data.supervisedSectionId").doesNotExist());
    }

    @Test
    void assignSection_nonexistentSection_returns404() throws Exception {
        var body = Map.of("sectionId", 99999L);

        mockMvc.perform(patch("/api/v1/users/{id}/section", instrAId)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignSection_targetIsStudent_returns400() throws Exception {
        var body = Map.of("sectionId", cs4910Id);

        mockMvc.perform(patch("/api/v1/users/{id}/section", studentId)
                        .header("Authorization", jwtHelper.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignSection_studentJwt_returns403() throws Exception {
        UserEntity student = new UserEntity();
        student.setFirstName("Bob"); student.setLastName("Jones"); student.setEmail("bob@tcu.edu");
        student.setRole(UserRole.STUDENT); student.setStatus(UserStatus.ACTIVE);
        student.setPassword("$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2");
        student.setEnabled(true);
        userRepository.save(student);

        var body = Map.of("sectionId", cs4910Id);
        mockMvc.perform(patch("/api/v1/users/{id}/section", instrAId)
                        .header("Authorization", jwtHelper.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }
}
