package edu.tcu.cs.projectpulse.war;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.user.UserEntity;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class WAREntryControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired WAREntryRepository warEntryRepository;
    @Autowired UserRepository userRepository;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;
    UserEntity student;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        warEntryRepository.deleteAll();

        student = new UserEntity();
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setEmail("war-test-" + System.currentTimeMillis() + "@tcu.edu");
        student.setRole("STUDENT");
        student = userRepository.save(student);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> entryPayload(String description) {
        return Map.of(
                "week", 1,
                "category", "DEVELOPMENT",
                "description", description,
                "plannedHours", 4.0,
                "actualHours", 3.5,
                "status", "In Progress"
        );
    }

    private Long createEntry(String description) throws Exception {
        String response = mockMvc.perform(post("/api/v1/war?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryPayload(description))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").longValue();
    }

    // ── GET /api/v1/war ──────────────────────────────────────────────────────

    @Test
    void getEntries_noEntries_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/war?week=1&userId=" + student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getEntries_withEntries_returnsOnlyMatchingWeek() throws Exception {
        createEntry("Week 1 task");

        // Week 2 entry via a second student (different week)
        mockMvc.perform(post("/api/v1/war?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "week", 2, "category", "TESTING",
                                "description", "Week 2 task",
                                "plannedHours", 2.0, "actualHours", 2.0,
                                "status", "Done"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/war?week=1&userId=" + student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].description").value("Week 1 task"));
    }

    // ── POST /api/v1/war ─────────────────────────────────────────────────────

    @Test
    void createEntry_validPayload_returns201WithData() throws Exception {
        mockMvc.perform(post("/api/v1/war?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryPayload("Build login page"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.description").value("Build login page"))
                .andExpect(jsonPath("$.data.category").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.data.status").value("In Progress"));
    }

    @Test
    void createEntry_blankDescription_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/war?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "week", 1, "category", "DEVELOPMENT",
                                "description", "",
                                "plannedHours", 4.0, "actualHours", 3.0,
                                "status", "In Progress"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createEntry_negativeHours_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/war?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "week", 1, "category", "DEVELOPMENT",
                                "description", "Some task",
                                "plannedHours", -1.0, "actualHours", 3.0,
                                "status", "In Progress"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createEntry_missingWeek_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/war?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "DEVELOPMENT",
                                "description", "Some task",
                                "plannedHours", 2.0, "actualHours", 2.0,
                                "status", "Done"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PUT /api/v1/war/{id} ─────────────────────────────────────────────────

    @Test
    void updateEntry_validPayload_returns200AndUpdates() throws Exception {
        Long id = createEntry("Original description");

        Map<String, Object> updated = Map.of(
                "week", 1, "category", "TESTING",
                "description", "Updated description",
                "plannedHours", 5.0, "actualHours", 5.0,
                "status", "Done"
        );

        mockMvc.perform(put("/api/v1/war/" + id + "?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.category").value("TESTING"))
                .andExpect(jsonPath("$.data.status").value("Done"));
    }

    @Test
    void updateEntry_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/war/9999?userId=" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryPayload("Doesn't matter"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── DELETE /api/v1/war/{id} ──────────────────────────────────────────────

    @Test
    void deleteEntry_exists_returns200AndRemoves() throws Exception {
        Long id = createEntry("To be deleted");

        mockMvc.perform(delete("/api/v1/war/" + id + "?userId=" + student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/war?week=1&userId=" + student.getId()))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void deleteEntry_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/war/9999?userId=" + student.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
