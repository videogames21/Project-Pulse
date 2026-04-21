package edu.tcu.cs.projectpulse.rubric;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class RubricControllerIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    RubricRepository rubricRepository;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        rubricRepository.deleteAll();
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private Map<String, Object> rubricPayload(String name, List<Map<String, Object>> criteria) {
        return Map.of("name", name, "criteria", criteria);
    }

    private Map<String, Object> criterion(String name, String description, double maxScore) {
        return Map.of("name", name, "description", description, "maxScore", maxScore);
    }

    private Long createRubric(String name) throws Exception {
        var payload = rubricPayload(name, List.of(criterion("Quality", "Desc", 10)));
        String response = mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").longValue();
    }

    // ── GET /api/v1/rubrics ─────────────────────────────────────────────────

    @Test
    void getAllRubrics_emptyList_returns200WithEmptyData() throws Exception {
        mockMvc.perform(get("/api/v1/rubrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getAllRubrics_withRubrics_returnsAllRubrics() throws Exception {
        createRubric("Rubric A");
        createRubric("Rubric B");

        mockMvc.perform(get("/api/v1/rubrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Rubric A", "Rubric B")));
    }

    // ── GET /api/v1/rubrics/{id} ────────────────────────────────────────────

    @Test
    void getRubricById_found_returns200WithDetails() throws Exception {
        Long id = createRubric("Peer Eval Rubric");

        mockMvc.perform(get("/api/v1/rubrics/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Peer Eval Rubric"))
                .andExpect(jsonPath("$.data.criteria", hasSize(1)))
                .andExpect(jsonPath("$.data.criteria[0].name").value("Quality"));
    }

    @Test
    void getRubricById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/rubrics/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /api/v1/rubrics ────────────────────────────────────────────────

    @Test
    void createRubric_validPayload_returns201AndPersists() throws Exception {
        var payload = rubricPayload("New Rubric", List.of(
                criterion("Quality", "How good is the work?", 10),
                criterion("Productivity", "How productive?", 10)
        ));

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Rubric"))
                .andExpect(jsonPath("$.data.criteria", hasSize(2)))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    void createRubric_duplicateName_returns409() throws Exception {
        createRubric("Duplicate Rubric");

        var payload = rubricPayload("Duplicate Rubric", List.of(criterion("X", "Y", 5)));

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createRubric_blankName_returns400() throws Exception {
        var payload = rubricPayload("", List.of(criterion("Quality", "Desc", 10)));

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createRubric_noCriteria_returns400() throws Exception {
        var payload = Map.of("name", "No Criteria Rubric", "criteria", List.of());

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createRubric_criterionBlankName_returns400() throws Exception {
        var payload = rubricPayload("Valid Name", List.of(criterion("", "Desc", 10)));

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createRubric_maxScoreZero_returns400() throws Exception {
        var payload = rubricPayload("Valid Name", List.of(criterion("Quality", "Desc", 0)));

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createRubric_maxScoreDecimal_returns201() throws Exception {
        var payload = rubricPayload("Decimal Rubric", List.of(criterion("Quality", "Desc", 9.5)));

        mockMvc.perform(post("/api/v1/rubrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.criteria[0].maxScore").value(9.5));
    }

    // ── PUT /api/v1/rubrics/{id} ────────────────────────────────────────────

    @Test
    void updateRubric_validPayload_returns200AndUpdates() throws Exception {
        Long id = createRubric("Old Name");

        var updated = rubricPayload("New Name", List.of(
                criterion("Updated Criterion", "Updated desc", 20)
        ));

        mockMvc.perform(put("/api/v1/rubrics/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.criteria", hasSize(1)))
                .andExpect(jsonPath("$.data.criteria[0].name").value("Updated Criterion"));
    }

    @Test
    void updateRubric_sameName_returns200() throws Exception {
        Long id = createRubric("Same Name");

        var payload = rubricPayload("Same Name", List.of(criterion("X", "Y", 5)));

        mockMvc.perform(put("/api/v1/rubrics/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Same Name"));
    }

    @Test
    void updateRubric_nameConflictWithOtherRubric_returns409() throws Exception {
        createRubric("Taken Name");
        Long id = createRubric("My Rubric");

        var payload = rubricPayload("Taken Name", List.of(criterion("X", "Y", 5)));

        mockMvc.perform(put("/api/v1/rubrics/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateRubric_notFound_returns404() throws Exception {
        var payload = rubricPayload("Doesn't Matter", List.of(criterion("X", "Y", 5)));

        mockMvc.perform(put("/api/v1/rubrics/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── DELETE /api/v1/rubrics/{id} ─────────────────────────────────────────

    @Test
    void deleteRubric_exists_returns200AndRemovesFromDb() throws Exception {
        Long id = createRubric("To Be Deleted");

        mockMvc.perform(delete("/api/v1/rubrics/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/rubrics/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRubric_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/rubrics/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
