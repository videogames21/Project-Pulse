package edu.tcu.cs.projectpulse.team;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class TeamControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired TeamRepository teamRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        teamRepository.deleteAll();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Long createTeam(String name, String sectionName) throws Exception {
        var body = Map.of("name", name, "sectionName", sectionName,
                "description", "Test desc", "websiteUrl", "");
        String response = mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").longValue();
    }

    // ── GET /api/v1/teams ────────────────────────────────────────────────────

    @Test
    void findAll_emptyList_returns200WithEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findAll_withTeams_returnsAllTeams() throws Exception {
        createTeam("Team Alpha", "CS4910");
        createTeam("Team Beta", "CS4910");

        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Team Alpha", "Team Beta")));
    }

    @Test
    void findAll_filterByTeamName_returnsMatchingTeams() throws Exception {
        createTeam("Team Alpha", "CS4910");
        createTeam("Team Beta", "CS4910");

        mockMvc.perform(get("/api/v1/teams?teamName=Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Team Alpha"));
    }

    @Test
    void findAll_filterBySectionName_returnsMatchingTeams() throws Exception {
        createTeam("Team Alpha", "CS4910");
        createTeam("Team Beta", "CS4911");

        mockMvc.perform(get("/api/v1/teams?sectionName=CS4910"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionName").value("CS4910"));
    }

    @Test
    void findAll_filterNoMatch_returnsEmptyArray() throws Exception {
        createTeam("Team Alpha", "CS4910");

        mockMvc.perform(get("/api/v1/teams?teamName=Nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ── GET /api/v1/teams/{id} ───────────────────────────────────────────────

    @Test
    void findById_found_returns200WithDetails() throws Exception {
        Long id = createTeam("Team Alpha", "CS4910");

        mockMvc.perform(get("/api/v1/teams/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.sectionName").value("CS4910"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/teams/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /api/v1/teams ───────────────────────────────────────────────────

    @Test
    void create_validPayload_returns200AndPersists() throws Exception {
        var body = Map.of("name", "New Team", "sectionName", "CS4910",
                "description", "A description", "websiteUrl", "http://example.com");

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Team"))
                .andExpect(jsonPath("$.data.sectionName").value("CS4910"))
                .andExpect(jsonPath("$.data.id").isNumber());

        assertThat(teamRepository.count()).isEqualTo(1);
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        createTeam("Duplicate Team", "CS4910");

        var body = Map.of("name", "Duplicate Team", "sectionName", "CS4911");

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        var body = Map.of("name", "", "sectionName", "CS4910");

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void create_blankSectionName_returns400() throws Exception {
        var body = Map.of("name", "Team X", "sectionName", "");

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PUT /api/v1/teams/{id} ───────────────────────────────────────────────

    @Test
    void update_validPayload_returns200AndUpdates() throws Exception {
        Long id = createTeam("Old Name", "CS4910");

        var body = Map.of("name", "New Name", "sectionName", "CS4911",
                "description", "Updated desc", "websiteUrl", "");

        mockMvc.perform(put("/api/v1/teams/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.sectionName").value("CS4911"));
    }

    @Test
    void update_sameName_returns200() throws Exception {
        Long id = createTeam("Same Name", "CS4910");

        var body = Map.of("name", "Same Name", "sectionName", "CS4910");

        mockMvc.perform(put("/api/v1/teams/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Same Name"));
    }

    @Test
    void update_nameConflictWithOtherTeam_returns409() throws Exception {
        createTeam("Taken Name", "CS4910");
        Long id = createTeam("My Team", "CS4910");

        var body = Map.of("name", "Taken Name", "sectionName", "CS4910");

        mockMvc.perform(put("/api/v1/teams/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        var body = Map.of("name", "Doesn't Matter", "sectionName", "CS4910");

        mockMvc.perform(put("/api/v1/teams/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
