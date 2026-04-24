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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class TeamInstructorAssignmentIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired TeamRepository teamRepository;
    @Autowired UserRepository userRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        userRepository.deleteAll();
        teamRepository.deleteAll();
    }

    private Long createTeam(String name) throws Exception {
        var body = Map.of("name", name, "sectionName", "2025-2026",
                "description", "", "websiteUrl", "");
        String res = mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).path("data").path("id").longValue();
    }

    private Long createInstructor(String name, String email) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(UserRole.INSTRUCTOR);
        return userRepository.save(u).getId();
    }

    private Long createStudent(String name, String email) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(UserRole.STUDENT);
        return userRepository.save(u).getId();
    }

    // ── POST /api/v1/teams/{id}/instructors ───────────────────────────────────

    @Test
    void assignInstructor_validRequest_returns200() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignInstructor_instructorAppearsInTeamResponse() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors", hasSize(1)))
                .andExpect(jsonPath("$.data.instructors[0].id").value(instructorId))
                .andExpect(jsonPath("$.data.instructors[0].name").value("Dr. Smith"))
                .andExpect(jsonPath("$.data.instructors[0].role").value("INSTRUCTOR"));
    }

    @Test
    void assignInstructor_teamNotFound_returns404() throws Exception {
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignInstructor_instructorNotFound_returns404() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", 9999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignInstructor_userIsStudent_returns400() throws Exception {
        Long teamId    = createTeam("Team Alpha");
        Long studentId = createStudent("Alice", "alice@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", studentId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignInstructor_alreadyAssigned_returns409() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long team2Id      = createTeam("Team Beta");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", team2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignInstructor_missingInstructorId_returns400() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignInstructor_multipleInstructors_allAppearInResponse() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long inst1Id = createInstructor("Dr. Smith",  "smith@tcu.edu");
        Long inst2Id = createInstructor("Dr. Jones",  "jones@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", inst1Id))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", inst2Id))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors", hasSize(2)));
    }

    @Test
    void getTeam_noInstructors_instructorsIsEmptyList() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors").isArray())
                .andExpect(jsonPath("$.data.instructors", hasSize(0)));
    }

    // ── DELETE /api/v1/teams/{id}/instructors/{instructorId} (UC-20) ─────────

    @Test
    void removeInstructor_validRequest_returns200() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teams/{id}/instructors/{iid}", teamId, instructorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void removeInstructor_instructorNoLongerInTeamResponse() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teams/{id}/instructors/{iid}", teamId, instructorId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructors", hasSize(0)));
    }

    @Test
    void removeInstructor_instructorBecomesAvailableForOtherTeam() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long team2Id      = createTeam("Team Beta");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teams/{id}/instructors/{iid}", teamId, instructorId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", team2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());
    }

    @Test
    void removeInstructor_teamNotFound_returns404() throws Exception {
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(delete("/api/v1/teams/{id}/instructors/{iid}", 9999L, instructorId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void removeInstructor_instructorNotFound_returns404() throws Exception {
        Long teamId = createTeam("Team Alpha");

        mockMvc.perform(delete("/api/v1/teams/{id}/instructors/{iid}", teamId, 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void removeInstructor_notAssignedToThisTeam_returns409() throws Exception {
        Long teamId  = createTeam("Team Alpha");
        Long team2Id = createTeam("Team Beta");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", team2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teams/{id}/instructors/{iid}", teamId, instructorId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignInstructor_studentsNotAffected() throws Exception {
        Long teamId       = createTeam("Team Alpha");
        Long studentId    = createStudent("Alice", "alice@tcu.edu");
        Long instructorId = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/{id}/students", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", java.util.List.of(studentId)))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/teams/{id}/instructors", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("instructorId", instructorId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(1)))
                .andExpect(jsonPath("$.data.instructors", hasSize(1)));
    }
}
